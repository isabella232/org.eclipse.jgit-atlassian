/*
 * Copyright (C) 2009, Google Inc.
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.lib;

import org.eclipse.jgit.util.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RepositoryShallowTest extends RepositoryTestCase {
	private static final String[] SHAS = {
		"49322bb17d3acc9146f98c97d078513228bbf3c0",
		"6e1475206e57110fcef4b92320436c1e9872a322",
		"1203b03dc816ccbb67773f28b3c19318654b0bc8",
		"bab66b48f836ed950c99134ef666436fb07a09a0",
	};
	private static final Set<ObjectId> ALL_SHAS = new HashSet<ObjectId>(toObjectIds(SHAS));
	private File shallowFile;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		shallowFile = new File(db.getDirectory(), "shallow");
	}

	@Test
	public void testReadingExistingFile() throws Exception {
		String contents = StringUtils.join(Arrays.asList(SHAS), "\n") + "\n";
		write(shallowFile, contents);
		assertEquals(ALL_SHAS, db.getShallows());
	}

	@Test
	public void testShallowHandleNoShallowFile() throws IOException {
		assertFalse(shallowFile.exists());

		Set<ObjectId> shallows = db.getShallows();

		assertTrue(shallows.isEmpty());

		db.addShallows(ALL_SHAS);
		assertEquals(ALL_SHAS, db.getShallows());
		assertEquals(ALL_SHAS, new HashSet<ObjectId>(readShallowFile()));
	}

	@Test
	public void testShallowHandleEmptyShallowFile() throws IOException {
		assertFalse(shallowFile.exists());
		assertTrue(shallowFile.createNewFile());

		Set<ObjectId> shallows = db.getShallows();

		assertTrue(shallows.isEmpty());

		db.addShallows(ALL_SHAS);
		assertEquals(ALL_SHAS, db.getShallows());
		assertEquals(ALL_SHAS, new HashSet<ObjectId>(readShallowFile()));
	}

	@Test
	public void testShallowHandleHalfFullShallowFile() throws IOException {
		write(shallowFile, SHAS[0] + "\n");

		Set<ObjectId> shallows = db.getShallows();
		assertEquals(Collections.singleton(ObjectId.fromString(SHAS[0])), shallows);

		db.addShallows(ALL_SHAS);
		assertEquals(ALL_SHAS, db.getShallows());
		assertEquals(ALL_SHAS, new HashSet<ObjectId>(readShallowFile()));
	}
	
	@Test
	public void testShallowHandleDisjointShallowFile() throws IOException {
		String newSha = "f73b95671f326616d66b2afb3bdfcdbbce110b44";
		write(shallowFile, newSha + "\n");
	
		Set<ObjectId> shallows = db.getShallows();

		assertEquals(Collections.singleton(ObjectId.fromString(newSha)), shallows);

		db.addShallows(ALL_SHAS);

		Set<ObjectId> expected = new HashSet<ObjectId>(ALL_SHAS);
		expected.add(ObjectId.fromString(newSha));
		assertEquals(expected, db.getShallows());
		assertEquals(expected, new HashSet<ObjectId>(readShallowFile()));
	}

	@Test
	public void testShallowHandleOverlappingShallowFile() throws IOException {
		String newSha = "f73b95671f326616d66b2afb3bdfcdbbce110b44";
		write(shallowFile, newSha + "\n" + SHAS[0] + "\n");

		db.addShallows(ALL_SHAS);

		Set<ObjectId> expected = new HashSet<ObjectId>(ALL_SHAS);
		expected.add(ObjectId.fromString(newSha));
		assertEquals(expected, db.getShallows());
		List<ObjectId> fromFile = readShallowFile();
		assertEquals(expected.size(), fromFile.size()); // verify no duplicates
		assertEquals(expected, new HashSet<ObjectId>(fromFile));
	}

	@Test
	public void testCacheEffective() throws Exception {
		String contents = StringUtils.join(Arrays.asList(SHAS), "\n") + "\n";
		write(shallowFile, contents);
		Set<ObjectId> s1 = db.getShallows();
		Set<ObjectId> s2 = db.getShallows();

		assertSame(s1, s2);

	}

	@Test
	public void testNotCreatingEmptyShallowFile() throws Exception {
		assertFalse(shallowFile.exists());
		db.addShallows(Collections.<ObjectId>emptySet());

		Set<ObjectId> shallows = db.getShallows();
		assertTrue(shallows.isEmpty());

		assertFalse(shallowFile.exists());
	}

	private static List<ObjectId> toObjectIds(String... shas) {
		List<ObjectId> objectIds = new ArrayList<ObjectId>(shas.length);
		for (String sha : shas) {
			objectIds.add(ObjectId.fromString(sha));
		}

		return objectIds;
	}

	private List<ObjectId> readShallowFile() throws IOException {
		List<ObjectId> contents = new ArrayList<ObjectId>();
		BufferedReader r = new BufferedReader(new FileReader(shallowFile));
		for (String line = r.readLine(); line != null; line = r.readLine()) {
			contents.add(ObjectId.fromString(line));
		}
		return contents;
	}

}
