
//Imports para usar as funções de sort e para que hajam as conversões implícitas para trabalhar com os integers 
import org.apache.spark.sql.functions._
import spark.implicits._

//Carga do arquivo jSON para um dataframe
val df = spark.read.json("/Users/flavio.clesio/Documents/spark-2.1.0/data/brasileiro_2015.json")

//Vamos ver o que tem dentro do nosso arquivo jSON
df.show()

//Para checar os datatypes, basta usar o método do dataframe chamado
df.printSchema()