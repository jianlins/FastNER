# FastNER

FastNER is an speed-optimized rule-base name entity recognition solution. It builds a hash-trie structure from rules and processing the input text without iterating through each of the rules.


## Maven setup
```xml
<dependency>
    <groupId>edu.utah.bmi.nlp</groupId>
    <artifactId>fastner</artifactId>
    <version>1.3.1.7</version>
</dependency>
```

## Examples

Use of FastNER is simple. Some example codes are here:

[Examples for token-based rules](https://github.com/jianlins/FastNER/blob/master/src/test/java/edu/utah/bmi/nlp/fastner/FastRuleWGTest.java)

[Examples for character-based rules](https://github.com/jianlins/FastNER/blob/master/src/test/java/edu/utah/bmi/nlp/fastcner/FastCNERTest.java)

