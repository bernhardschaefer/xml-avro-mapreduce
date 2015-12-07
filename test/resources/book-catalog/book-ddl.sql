CREATE EXTERNAL TABLE book
STORED AS AVRO
LOCATION '/data/book-catalog/processed/'
TBLPROPERTIES ('avro.schema.url'='hdfs:///data/book-catalog/metadata/book.avsc');