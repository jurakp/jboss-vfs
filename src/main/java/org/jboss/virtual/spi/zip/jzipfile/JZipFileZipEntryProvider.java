/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.virtual.spi.zip.jzipfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.jboss.jzipfile.Zip;
import org.jboss.jzipfile.ZipCatalog;
import org.jboss.jzipfile.ZipEntryType;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.spi.zip.ZipEntry;
import org.jboss.virtual.spi.zip.ZipEntryProvider;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class JZipFileZipEntryProvider implements ZipEntryProvider
{
   private Iterator<org.jboss.jzipfile.ZipEntry> entries;
   private File tempFile;
   private org.jboss.jzipfile.ZipEntry current;

   public JZipFileZipEntryProvider(InputStream is) throws IOException
   {
      if (is == null)
         throw new IllegalArgumentException("Null input stream");

      final File tempFile = File.createTempFile("jboss-vfs-jzfzep-", ".zip");
      tempFile.deleteOnExit();
      final FileOutputStream os = new FileOutputStream(tempFile);
      VFSUtils.copyStreamAndClose(is, os);

      ZipCatalog catalog = Zip.readCatalog(tempFile);
      entries = catalog.allEntries().iterator();
      this.tempFile = tempFile;
   }

   public ZipEntry getNextEntry() throws IOException
   {
      if (entries.hasNext()) {
         return new JZipFileZipEntry(current = entries.next());
      } else {
         current = null;
         tempFile.delete();
         return null;
      }
   }

   private static final InputStream EMPTY_STREAM = new InputStream()
   {
      public int read() throws IOException
      {
         return -1;
      }
   };

   public InputStream currentStream() throws IOException
   {
      final org.jboss.jzipfile.ZipEntry current = this.current;
      return current == null ? null : current.getEntryType() == ZipEntryType.FILE ? Zip.openEntry(tempFile, current) : EMPTY_STREAM;
   }

   public void close() throws IOException
   {
      current = null;
   }
}