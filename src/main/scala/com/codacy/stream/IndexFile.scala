package com.codacy.stream

import net.openhft.chronicle.bytes.MappedFile
import net.openhft.chronicle.core.OS

import java.io.{File, RandomAccessFile}
// import java.io.FileNotFoundException;
// import java.io.RandomAccessFile;

object IndexFile {

  def of(file: File, chunkSize: Long, overlapSize: Long, capacity: Long): IndexFile =
    new IndexFile(file, new RandomAccessFile(file, "rw"), chunkSize, overlapSize, capacity)

  def of(file: File, chunkSize: Long, overlapSize: Long): IndexFile =
    new IndexFile(file, new RandomAccessFile(file, "rw"), chunkSize, overlapSize, OS.pageSize())

  def of(file: File, chunkSize: Long): IndexFile =
    of(file, chunkSize, OS.pageSize())

  def of(filename: String, chunkSize: Long, overlapSize: Long): IndexFile =
    of(new File(filename), chunkSize, overlapSize)
}

class IndexFile(file: File, raf: RandomAccessFile, chunkSize: Long, overlapSize: Long, capacity: Long)
    extends MappedFile(file, raf, chunkSize, overlapSize, capacity, false)
