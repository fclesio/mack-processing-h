//1) Quem foi o time que terminou em nono lugar?
scala> spark.sql("SELECT * FROM brasileiro WHERE Pos =9").show()

//2) Quem são os times que ficaram entre o 12 lugar e o 19 lugar?
scala> spark.sql("SELECT * FROM brasileiro WHERE Pos BETWEEN 12 AND 19").show()

//3) Quem foram os times que ficaram com mais de 50 pontos?
scala> spark.sql("SELECT * FROM brasileiro WHERE p > 50").show()

//4) Quais foram os times que tiveram mais de 50 gols pró (GP)?
scala> spark.sql("SELECT * FROM brasileiro WHERE GP > 50").show()

//5) Quais foram os times que tiveram menos de 40 pontos, e tiveram mais de 40 gols pró?
scala> spark.sql("SELECT * FROM brasileiro WHERE GP > 40 AND p < 40").show()

//6) Quais foram os times que ficaram entre quarto e decimo lugar, que tiveram mais de 40 gols pró, e tiveram mais de 40 pontos?
scala> spark.sql("SELECT * FROM brasileiro WHERE Pos BETWEEN 4 AND 10 AND p > 40 AND GP > 40 ").show()

//7) Qual foi a pontuação dos rebaixados (17+)?
scala> spark.sql("SELECT time, Pos, p FROM brasileiro WHERE Pos >= 17").show()

