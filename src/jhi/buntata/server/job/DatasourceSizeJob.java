/*
 * Copyright 2017 Information & Computational Sciences, The James Hutton Institute
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

package jhi.buntata.server.job;

import java.io.*;
import java.util.*;
import java.util.function.*;

import jhi.buntata.data.*;
import jhi.buntata.resource.*;

/**
 * This {@link Runnable} updates the data size information of all {@link BuntataDatasource} objects by checking their {@link BuntataMedia} objects and
 * summing over their size.
 */
public class DatasourceSizeJob implements Runnable
{
	private final DatasourceDAO datasourceDAO = new DatasourceDAO();
	private final NodeDAO       nodeDAO       = new NodeDAO();
	private final MediaDAO      mediaDAO      = new MediaDAO();

	private Set<String> alreadyCounted = new HashSet<>();

	private Predicate<File> filter = f ->
	{
		// Has to exist and be a file
		boolean result = f.exists() && f.isFile();

		// Add it if it didn't already exist
		result &= alreadyCounted.add(f.getAbsolutePath());

		// Result is only true if the file exists, is a file (not a folder) and hasn't already been counted
		return result;
	};

	@Override
	public void run()
	{
		alreadyCounted.clear();

		// Get all the data sources
		List<BuntataDatasource> datasources = datasourceDAO.getAll(true);

		for (BuntataDatasource datasource : datasources)
		{
			long sizeTotal = 0;
			long sizeNoVideo = 0;

			// Get all the nodes
			List<BuntataNode> nodes = nodeDAO.getAllForDatasource(datasource.getId());

			for (BuntataNode node : nodes)
			{
				// Get all the media
				Map<String, List<BuntataMedia>> media = mediaDAO.getAllForNode(node.getId(), true);

				long imageSize = media.get(BuntataMediaType.TYPE_IMAGE)
									  .stream()
									  .map(m -> new File(m.getInternalLink()))
									  .filter(filter)
									  .map(File::length)
									  .mapToLong(Long::longValue)
									  .sum();

				long videoSize = media.get(BuntataMediaType.TYPE_VIDEO)
									  .stream()
									  .map(m -> new File(m.getInternalLink()))
									  .filter(filter)
									  .map(File::length)
									  .mapToLong(Long::longValue)
									  .sum();

				sizeTotal += imageSize + videoSize;
				sizeNoVideo += imageSize;
			}

			// Only save changes if the values actually changed. This prevents the "updated_on" field to be modified if nothing really changed.
			if (sizeTotal != datasource.getSizeTotal() || sizeNoVideo != datasource.getSizeNoVideo())
			{
				datasource.setSizeTotal(sizeTotal);
				datasource.setSizeNoVideo(sizeNoVideo);
				// And save
				datasourceDAO.updateSize(datasource);
			}
		}
	}
}