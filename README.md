# FastNER

FastNER is an speed-optimized rule-base name entity recognition solution. It uses n-trie engine<sup>[1]</sup> internally to builds a hash-trie structure from rules and processing the input text without iterating through every rule.


## Maven setup
```xml
<dependency>
    <groupId>edu.utah.bmi.nlp</groupId>
    <artifactId>fastner</artifactId>
    <version>1.3.1.8</version>
</dependency>
```

## Examples

Use of FastNER is simple. Some example codes are here:

[Examples for token-based rules](https://github.com/jianlins/FastNER/blob/master/src/test/java/edu/utah/bmi/nlp/fastner/FastRuleWGTest.java)

[Examples for character-based rules](https://github.com/jianlins/FastNER/blob/master/src/test/java/edu/utah/bmi/nlp/fastcner/FastCNERTest.java)

## References

If you are using FastNER within your research work, please cite the following publication:

1. Shi, Jianlin, and John F. Hurdle. “Trie-Based Rule Processing for Clinical NLP: A Use-Case Study of n-Trie, Making the ConText Algorithm More Efficient and Scalable.” Journal of Biomedical Informatics, August 6, 2018. https://doi.org/10.1016/j.jbi.2018.08.002.
