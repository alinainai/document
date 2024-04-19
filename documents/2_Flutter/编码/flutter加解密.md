1、实现RSA加解密

```dart
 static String rsaEncrypt(String content, String publicKey) {
    dynamic parser = RSAKeyParser().parse(publicKey);
    final encryptor = Encrypter(RSA(publicKey: parser));

    List<int> sourceBytes = utf8.encode(content);
    int inputLen = sourceBytes.length;
    int maxLen = 117;
    List<int> totalBytes = <int>[];
    for (var i = 0; i < inputLen; i += maxLen) {
      int endLen = inputLen - i;
      List<int> item;
      if (endLen > maxLen) {
        item = sourceBytes.sublist(i, i + maxLen);
      } else {
        item = sourceBytes.sublist(i, i + endLen);
      }
      totalBytes.addAll(encryptor.encryptBytes(item).bytes);
    }
    return base64.encode(totalBytes);
  }

  // RSA 解密函数
  static String rsaDecrypt(String content, String privateKey) {
    dynamic parser = RSAKeyParser().parse(privateKey);
    final encryptor = Encrypter(RSA(privateKey: parser));

    Uint8List sourceBytes = base64.decode(content);
    int inputLen = sourceBytes.length;
    int maxLen = 128;
    var totalBytes = <int>[];
    for (var i = 0; i < inputLen; i += maxLen) {
      int endLen = inputLen - i;
      Uint8List item;
      if (endLen > maxLen) {
        item = sourceBytes.sublist(i, i + maxLen);
      } else {
        item = sourceBytes.sublist(i, i + endLen);
      }
      totalBytes.addAll(encryptor.decryptBytes(Encrypted(item)));
    }
    return utf8.decode(totalBytes);
  }
```








