function fn() {
  var env = karate.env || "dev";
  var connectTimeout = karate.properties["connectTimeout"] || 45000;

  var baseUrl =
    karate.properties["baseUrl"] ||
    karate.properties["BASE_URL"] ||
    "http://localhost:8080";

  var config = {
    baseUrl
  };

  karate.log("karate.env system property was: ", env);

  karate.configure('connectTimeout', connectTimeout);
  karate.configure('readTimeout', connectTimeout);
  karate.configure('ssl', true);
  return config;
}