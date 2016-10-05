/*******************************************************************************
 * Copyright (C) 2014 xperia64 <xperiancedapps@gmail.com>
 * 
 * Copyright (C) 1999-2008 Masanao Izumo <iz@onicos.co.jp>
 *     
 * Copyright (C) 1995 Tuukka Toivonen <tt@cgs.fi>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.util;

import java.io.File;
import java.text.Collator;
import java.util.Comparator;

public class FileComparator implements Comparator<Object> {
	private Collator c = Collator.getInstance();

	public int compare(Object o1, Object o2) {
		if (o1 == o2)
			return 0;
		File f1 = (File) o1;
		File f2 = (File) o2;

		if (f1.isDirectory() && f2.isFile())
			return -1;
		if (f1.isFile() && f2.isDirectory())
			return 1;

		return c.compare(f1.getName(), f2.getName());
	}
}
