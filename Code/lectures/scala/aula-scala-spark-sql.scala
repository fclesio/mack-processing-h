/*

Iternals da solução e do Core
Desenvolvedores e Arquitetos


Spark SQL
- Conceito de sessão no Spark
- Evolução: RDD, Dataframes, Datasets
- Deployment em pipelines de ETL e processamento em Big Data


Arquitetura do Internals


//Alto nível

ML Pipelines | Structured Stramming | Graph Frames
ANSI SQL Parser | Dataframes & Datasets
Catalyst Optimizer
Spark-Core (RDD)

//Baixo nível 


A contar da distribuição do 2.0 foi feita a unificação da API Dataframes com a Datasets para facilitar o desenvolvimento
de pipelines de ETL em uma linguagem unificada em mais alto nível, porém, as instruções em RDD continam ativas para quem 
quiser desenvolver em mais baixo nível. 

Catalyst Optimizer
- API amigável com o Scala o que dá mais possibilidades em termos de integração de código
- Realização de planos de execução e otimização de acordo com os recursos do cluster e dos nós
- Não tem perda de performance usando outras linguagens (Meia verdade, há indicios que indicam o contrário - Vide palestra da Holden Karau no Strange Loop)
- O código é enviado para o Calayst que faz a tradução e manda para o RDD-Core em baixo nível

Contexto
- Antigamente cada módulo do Spark tinha inúmeros contextos como SparkSession, SQLContext, HiveContext
- Os contextos de desenvolvimento invocavam módulos separados para operações específicas
- Isso poupava tempo de desenvolvimento, mas deixava o código mais verboso e as vezes com contextos conflitantes (HiveContext e SQLContext)
- O SparkSession equalizou isso e todos os contextos estão juntos e sobem ao mesmo tempo no Spark


*/


//Ajustes nas configurações do Spark via Spark-Shell (O ideal é sempre fazer no arquivo de configuracão apresentado na aula anterior)
spark.conf.set("spark.executor.cores", "2")    
spark.conf.set("spark.executor.memory", "4g")

//AULA 1 - Basics 


//Transformation
//Action

val rdd_one = sc.parallelize(Seq(1,2,3,4,5,6))

rdd_one.take(100)

val rdd_two = rdd_one.map(i => i * 3)

rdd_two.take(10)

val rdd_three = rdd_two.map(i => i+2)

rdd_three.take(10)



rdd_one.toDebugString

rdd_two.toDebugString

rdd_three.toDebugString




val rdd_four = rdd_three.map(i => ("str"+(i+2).toString, i-2))

rdd_four.take(10)


//DOUBLE RDD - RDD collection with double values
val rdd_one = sc.parallelize(Seq(1.0,2.0,3.0))

rdd_one.mean

rdd_one.min

rdd_one.max

rdd_one.stdev


//UNION RDD
val rdd_one = sc.parallelize(Seq(1,2,3))

val rdd_two = sc.parallelize(Seq(4,5,6))



val rdd_one = sc.parallelize(Seq(1,2,3))

rdd_one.take(10)

val rdd_two = sc.parallelize(Seq(4,5,6))

rdd_two.take(10)



val unionRDD = rdd_one.union(rdd_two)

unionRDD.take(10)












//Vamos chamar a parte do Spark SQL que faz a determinação dos data types
import org.apache.spark.sql.types._

//Nessa trecho do códico vamos criar um schema novo do tipo StructType em que vamos adicionar as nossas colunas já com o nome e o seu respectivo tipo 
val recordSchema = new StructType()
	.add("sample", "long")
	.add("cThick", "integer")
	.add("uCSize", "integer")
	.add("uCShape", "integer")
	.add("mAdhes", "integer")
	.add("sECSize", "integer")
	.add("bNuc", "integer")
	.add("bChrom", "integer")
	.add("nNuc", "integer")
	.add("mitosis", "integer")
	.add("clas", "integer")

//Vamos determinar um caminho padrão somente para que o código fique menos verboso 
// val ROOT_DIR = "/Users/flavio.clesio/Documents/spark-2.1.0/data/"
val ROOT_DIR = "/Users/flavio.clesio/Desktop/"


//Neste snippet vamos criar um dataframe em que vamos passar a leitura de um arquivo csv
//que o spark implicitamente sabe que é delimitado por virgula, sem o header, usando o nosso schema determinado anteriormente,
//e carregando os dados 
val df = spark.read.format("csv")
	.option("header", false)
	.schema(recordSchema)
	.load(ROOT_DIR + "breast-cancer-wisconsin.data")

//Link: https://archive.ics.uci.edu/ml/machine-learning-databases/breast-cancer-wisconsin/breast-cancer-wisconsin.data

//Vamos chamar a função createOrReplaceTempView() criar uma view temporária durante o contexto do spark
//para dropar posteriormente basta chamar a função spark.catalog.dropTempView("cancerTable") 
createOrReplaceTempView()

//Neste step invocamos a criaão da view com o nome de cancerTable
df.createOrReplaceTempView("cancerTable") 
 
//Com a nossa view criada, podemos realizar queries e armazenar em objetos do scala usando 
//o método .sql da classe spark 
val sqlDF = spark.sql("SELECT sample, bNuc from cancerTable") 

//A vantagem de usar os Dataframes e datasets do Spark unificados é de uma certa com isso há uma alternativa de se dar um 
//"wannabe" by pass no lazyness do Scala; isto é, o Data Scientist, Developer ou quem quer que esteja mexendo no código
//conegue fazer alguns debugs intermediários antes de enviar para a função posterior
//e alem disso abre um leque para testes tanto unitários quando de integração no pipeline de ETL
//mesmo com o lazyness do Scala. Em outras palavras: Usa-se o lazyness do scala para não precisarmos nos preocupar com 
//runtime, e podemos realizar testes em tempo de codagem para verificar anomalias no codigo. Ou seja: 
//Mais bug-free do que isso é quase impossível. 

//Aqui vamos criar uma case class. Case classe serve geralmente para a criação de dados imutáveis
//A.K.A. não teremos alteração no schema e nem nos data types posteriormente. Outra feature é que as case classes
//implementam apply por default, o que significa que inumeras instancias das classes podem ser criadas sem precisar do parametro new
//AVISO: Até hoje (2017-10-14 20:40:57) case class ainda nao tem heranca de classe, o que pode chatear parte dos desenvolvedores
//para saber mais clique no link abaixo
//Heranca de Case Class: https://stackoverflow.com/questions/11158929/what-is-so-wrong-with-case-class-inheritance
//Para saber mais sobre case class, esse link é uma ótima referência: http://www.alessandrolacava.com/blog/scala-case-classes-in-depth/
case class CancerClass(sample: Long, cThick: Int, uCSize: Int, uCShape: Int, mAdhes: Int, sECSize: Int, bNuc: Int, bChrom: Int, nNuc: Int, mitosis: Int, clas: Int)

//Agora criaremos o nosso dataset usando o map() para realizar a quebra das lnhas, e posteriormente declarando os atributos e as suas respectivas
//posicoes na case class CancerClass 
val cancerDS = spark.sparkContext.textFile(ROOT_DIR + "breast-cancer-wisconsin.data").map(_.split(",")).map(attributes => CancerClass(attributes(0).trim.toLong, attributes(1).trim.toInt, attributes(2).trim.toInt, attributes(3).trim.toInt, attributes(4).trim.toInt, attributes(5).trim.toInt, attributes(6).trim.toInt, attributes(7).trim.toInt, attributes(8).trim.toInt, attributes(9).trim.toInt, attributes(10).trim.toInt)).toDS()
 
//Essa funcao nos passamos o parametro 2 que sera um int, em que se houver um match de 2 ele vai 
//passar o numero para zero e no caso de haver o match de 4 ele vai passar para 1  
def binarize(s: Int): Int = s match {case 2 => 0 case 4 => 1 }
 
//Vamos registrar agora a funcao definida pelo usuario (UDF)  
spark.udf.register("udfValueToCategory", (arg: Int) => binarize(arg))
 
//Em seguida vamos criar um novo objeto em que passamos a funcao dentro de uma consulta SQL para retornar um resultado 
val sqlUDF = spark.sql("SELECT *, udfValueToCategory(clas) from cancerTable")

//Vamos verificar agora o resultado  
sqlUDF.show()

//Vamos checar o database do catalogo
spark.catalog.currentDatabase

//Checa se a tabela esta no cache de memoria
spark.catalog.isCached("cancerTable") 

//Faz a inclusao da tabela no chache
spark.catalog.cacheTable("cancerTable") 

//Checa se a tabela esta no cache de memoria
spark.catalog.isCached("cancerTable") 

//Limpeza do cache
spark.catalog.clearCache 

//Checa se a tabela esta no cache de memoria
spark.catalog.isCached("cancerTable") 

//Lista os databases do catálogo
spark.catalog.listDatabases.show()

//Pega o primeiro database do catalogo
spark.catalog.listDatabases.take(1)

//Lista todas as tabelas do catalogo
spark.catalog.listTables.show()

//Vamos agora dropar a tabela
spark.catalog.dropTempView("cancerTable")

//Vejamos quais tabelas estao listadas
spark.catalog.listTables.show()






//AULA 2 - RDD, Dataframes e Datasets
/*
RDD: Imutável, distribuído, avaliado de forma lazy, tipado, cacheado, e tem um DAG de execução próprio

Importantissimo sobre o DAG: O Spark é composto de uma série de camadas de execução no momento em que algum job é submetido
em geral funciona DAG (Directed Acyclic Graph)

1) Scala Interpreter faz o trabalho de parser e validação do código em Scala;
2) O Spark faz a criação do operador grafo para calcular a rota de processamento ao longo dos nós
3) Em qualquer ação o grafo faz a submissão para o DAG realizar a validação
4) O DAG faz a quebra de tarefas/stages de map() e reduce() ao longo das partições
5) Os stages sao passados para o task scheduler através do cluster manager (Yarn/Mesos/Standalone)
6) Os workers executam as tarefas dos slaves  e a JVM inicia o job
7) Ao longo dessas computacoes sucessivas para cada um dos stages, o plano de execucao e otimizado e reduz-se o 

Melhores referência sobre esse internals
1) http://data-flair.training/blogs/dag-in-apache-spark/
2) https://www.sigmoid.com/apache-spark-internals/
3) Um ótimo usecase: https://www.quora.com/What-exactly-is-Apache-Spark-and-how-does-it-work-What-does-a-cluster-computing-system-mean/answer/Prithiviraj-Damodaran



*/

val cancerRDD = sc.textFile(ROOT_DIR + "breast-cancer-wisconsin.data", 4)

import spark.implicits._

cancerRDD.partitions.size

val cancerDF = cancerRDD.toDF()



/*
Dataframes e Datasets

Interface mais amigável para os Cientistas de Dados
Formato tabular
Ainda passa pelo interpretador do Catalyst 
Contém todas as features legais do RDD
Tem manipulação de dados distribuída via DSL (Domain-specific language)
Bem similar ao paradigma do relacional dos SGBDs
Abstraí muito do Scala em termos de código (i.e. isso significa MUITO para uma linguagem tão complexa quanto o Scala e pode evitar pesadelos como esse: https://movio.co/en/blog/migrate-Scala-to-Go/ e esse http://jimplush.com/talk/2015/12/19/moving-a-team-from-scala-to-golang/)

*/

// Na conversão de RDDs para Dataframes, esse comando realiza a conversão implicita. 
import spark.implicits._

//Criando Dataframes
val df = spark.read.json("examples/src/main/resources/people.json")

df.show()


//Operações com Dataframes
import spark.implicits._

df.printSchema()

//Operações com unTyped

//Selecionando apenas a coluna nome
df.select("name").show()


//Seleciona todos os elementos e aumenta a idade em um ano
df.select($"name", $"age" + 1).show()

df.select($"name", $"age", $"age" + 1).show()

//Filtra pessoas com mais de 21 anos
df.filter($"age" > 21).show()

//Agrupa via contagem todas as pessoas pela idade
df.groupBy("age").count().show()







//Programação via SQL no Scala


//Instanciamento de view temporária
df.createOrReplaceTempView("people")

val sqlDF = spark.sql("SELECT * FROM people")

sqlDF.show()

//Instanciamento de view global
df.createGlobalTempView("people")

// Consulta na view atrelado em uma base de dados no banco de dados 'global.temp'
spark.sql("SELECT * FROM global_temp.people").show()

// Com a consulta abaixo criamos uma sessão nova, e mesmo assim conseguimos acessar a view. 
spark.newSession().sql("SELECT * FROM global_temp.people").show()






//Criando Datasets no Scala

// Classe customizada pessoa
case class Person(name: String, age: Long)

// Usando encoders para criar classes case
val caseClassDS = Seq(Person("Andy", 32)).toDS()

caseClassDS.show()


// Com os enconders grande parte dos datatypes são parseados automáticamente devido ao comando importing spark.implicits._
val primitiveDS = Seq(1, 2, 3).toDS()

primitiveDS.map(_ + 1).collect() 

// Os DataFrames podem ser convertidos para um Dataset que tenha uma classe. 
val path = "examples/src/main/resources/people.json"

val peopleDS = spark.read.json(path).as[Person]

peopleDS.show()







//Interoperabilidade com os RDDs

//Inferência de esquema usando reflection
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.Encoder

// Comando para realizar conversões implicitas do RDD para Dataframe
import spark.implicits._

// Agora vamos criar um RDD do objeto person de um arquivo texto e converter isso para um dataframe
val peopleDF = spark.sparkContext
  .textFile("examples/src/main/resources/people.txt")
  .map(_.split(","))
  .map(attributes => Person(attributes(0), attributes(1).trim.toInt))
  .toDF()

// Vamos registrar o DataFrame como uma view temporária
peopleDF.createOrReplaceTempView("people")

// Agora vamos criar um objeto no Scala usando uma consulta SQL em que vamos buscar todos os adolescentes
val teenagersDF = spark.sql("SELECT name, age FROM people WHERE age BETWEEN 13 AND 19")

// As colunas na linha podem ser acessadas por um campo index
teenagersDF.map(teenager => "Name: " + teenager(0)).show()

// Ou pelo nome do campo
teenagersDF.map(teenager => "Name: " + teenager.getAs[String]("name")).show()

// Quando não há encoders predefinidos para o dataset baseado em Key-Value Dataset[Map[K,V]] esse comando faz a definição de forma explicita.
implicit val mapEncoder = org.apache.spark.sql.Encoders.kryo[Map[String, Any]]










//Objeto Key/Value para geracao de sequenceFile
val data = sc.parallelize(List(("Luis", 1), ("Carmen", 2), ("Gilmar", 2)))

//Conteudo do arquivo 
data.first()

//Gravacao do arquivo
data.saveAsTextFile(ROOT_DIR + "spark-files-saved")

//Carga do arquivo 
val result = sc.textFile(ROOT_DIR + "spark-files-saved/part-0000*").map(_.split(","))


var result2 = result.toDF()


import sqlContext.implicits._
// rdd.toDF()


val schema = new StructType()
  .add(StructField("nome", StringType, true))
  .add(StructField("val1", DoubleType, true))



// val df = spark.createDataFrame(result, schema)


val rdd = sc.makeRDD(result)

// Comando para realizar conversões implicitas do RDD para Dataframe
import spark.implicits._

// Agora vamos criar um RDD do objeto person de um arquivo texto e converter isso para um dataframe
val peopleDF = spark.sparkContext
  .textFile(ROOT_DIR + "spark-files-saved/part-0000*")
  .map(_.split(","))
  .map(attributes => Person(attributes(0), attributes(1).trim.toInt))
  .toDF()


