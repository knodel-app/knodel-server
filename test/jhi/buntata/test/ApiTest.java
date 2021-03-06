/*
 * Copyright 2018 Information & Computational Sciences, The James Hutton Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jhi.buntata.test;

import org.junit.jupiter.api.*;

import java.io.*;
import java.util.*;

import jhi.buntata.resource.*;
import okhttp3.*;
import retrofit2.Response;
import retrofit2.*;
import retrofit2.converter.jackson.*;

/**
 * @author Sebastian Raubach
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApiTest extends DatabaseTest
{
	private ApiProvider               provider;
	private String                    bearerToken;
	private OkHttpClient              client;
	private AuthenticationInterceptor interceptor;

	public void prepareProvider(OkHttpClient client)
	{
		Retrofit.Builder builder = new Retrofit.Builder()
			.baseUrl("http://localhost:8080/" + tomcatName + "/v1.1/")
			.addConverterFactory(JacksonConverterFactory.create());

		if (client != null)
			builder.client(client);

		provider = builder
			.build()
			.create(ApiProvider.class);
	}

	private void prepareClient(boolean auth)
	{
		if (auth)
		{
			interceptor.setAuthToken("Bearer", bearerToken);
		}
		else
		{
			interceptor.setAuthToken(null, null);
		}
	}

	@BeforeAll
	public void getToken()
		throws IOException
	{
		String basicToken = Credentials.basic(masterUsername, masterPassword);

		interceptor = new AuthenticationInterceptor(basicToken);
		OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
		httpClient.addInterceptor(interceptor);

		client = httpClient.build();
		prepareProvider(client);

		Response<BuntataToken> response = provider.token(new BuntataUser()
			.setUsername(masterUsername)
			.setPassword(masterPassword))
												  .execute();

		assert response.body() != null;
		this.bearerToken = response.body().getToken();
	}

	@Test
	public void putDatasourceWithoutAuth()
		throws IOException
	{
		prepareClient(false);
		prepareProvider(null);

		BuntataDatasource datasource = new BuntataDatasource(1L, new Date(), new Date())
			.setName("Datasource 1")
			.setContact("Sebastian Raubach")
			.setDataProvider("The James Hutton Institute")
			.setShowKeyName(true)
			.setShowSingleChild(false)
			.setVisibility(true)
			.setIcon("myicon.png");

		Response<ResponseBody> response = provider.putDatasource("1", datasource).execute();

		assert !response.isSuccessful();
		assert response.code() == 403;
	}

	@Test
	public void putDatasourceWithAuth()
		throws IOException
	{
		prepareClient(true);
		prepareProvider(client);

		BuntataDatasource datasource = new BuntataDatasource(1L, new Date(), new Date())
			.setName("Datasource 1")
			.setContact("Sebastian Raubach")
			.setDataProvider("The James Hutton Institute")
			.setShowKeyName(true)
			.setShowSingleChild(false)
			.setVisibility(true)
			.setIcon("myicon.png");
		Response<ResponseBody> response = provider.putDatasource("1", datasource).execute();

		assert response.isSuccessful();
		assert response.code() == 200;
		assert response.body() != null;
		assert isLong(response.body().string());
	}

	@Test
	public void putNodeWithAuth()
		throws IOException
	{
		prepareClient(true);
		prepareProvider(client);

		BuntataNode node = new BuntataNode(1L, new Date(), new Date())
			.setName("Node 1")
			.setDescription("Some node description")
			.setDatasourceId(1L);

		Response<ResponseBody> response = provider.putNode("1", node).execute();

		assert response.isSuccessful();
		assert response.code() == 200;
		assert response.body() != null;
		assert isLong(response.body().string());
	}

	@Test
	@Disabled
	public void postNodeWithAuth()
		throws IOException
	{
		prepareClient(true);
		prepareProvider(client);

		BuntataNode node = new BuntataNode(null, new Date(), new Date())
			.setName("Node 1")
			.setDescription("Some node description")
			.setDatasourceId(1L);

		Response<ResponseBody> response = provider.postNode(node).execute();

		assert response.isSuccessful();
		assert response.code() == 200;
		assert response.body() != null;
		assert isLong(response.body().string());
	}

	@Test
	public void checkDatasources()
		throws IOException
	{
		prepareClient(false);
		prepareProvider(null);

		Response<List<BuntataDatasource>> response = provider.getDatasources().execute();

		assert response.isSuccessful();
		assert response.code() == 200;
		assert response.body() != null;
		assert response.body().size() == 1;
	}

	private boolean isLong(String value)
	{
		try
		{
			Long.parseLong(value);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
