/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.Test;

import java.io.IOException;

/**
 * @author fisher
 */
public class ElasticSearchTest {

    @Test
    public void testSearch() throws IOException {
        // 连接本地启动的 ES
        RestClient client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"))
            .build();

        // 发一个 _search 请求
        Request request = new Request("GET", "/_cluster/health");
        Response response = client.performRequest(request);

        System.out.println(response.getStatusLine());
        System.out.println(new String(response.getEntity().getContent().readAllBytes()));
        client.close();
    }

    @Test
    public void write() throws IOException {
        // 连接本地启动的 ES
        RestClient client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"))
            .build();

        Request indexRequest = new Request("POST", "/test_index/_doc/1");
        indexRequest.setJsonEntity("""
            {
              "name": "Alice",
              "age": 30,
              "city": "Singapore"
            }
            """);
        Response indexResponse = client.performRequest(indexRequest);
        System.out.println(new String(indexResponse.getEntity().getContent().readAllBytes()));
        client.close();
    }

    @Test
    public void query() throws IOException {
        // 连接本地启动的 ES
        RestClient client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"))
            .build();

        Request getRequest = new Request("GET", "/test_index/_doc/1");
        Response getResponse = client.performRequest(getRequest);
        System.out.println(new String(getResponse.getEntity().getContent().readAllBytes()));
        client.close();
    }

    @Test
    public void batch() throws IOException {
        // 连接本地启动的 ES
        RestClient client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"))
            .build();

        Request bulkRequest = new Request("POST", "/_bulk");
        bulkRequest.setJsonEntity("""
        { "index" : { "_index" : "test_index", "_id" : "2" } }
        { "name" : "Bob", "age" : 25 }
        { "index" : { "_index" : "test_index", "_id" : "3" } }
        { "name" : "Charlie", "age" : 28 }
        """);
        Response bulkResponse = client.performRequest(bulkRequest);
        System.out.println(new String(bulkResponse.getEntity().getContent().readAllBytes()));
        client.close();
    }

    @Test
    public void index() throws IOException {
        // 连接本地启动的 ES
        RestClient client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"))
            .build();

        Request createIndex = new Request("PUT", "/test_index");
        createIndex.setJsonEntity("""
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0
              },
              "mappings": {
                "properties": {
                  "name": { "type": "keyword" },
                  "age": { "type": "integer" },
                  "city": { "type": "keyword" }
                }
              }
            }
            """);
        Response response = client.performRequest(createIndex);
        System.out.println(new String(response.getEntity().getContent().readAllBytes()));
        client.close();
    }

    @Test
    public void queryAny() throws IOException {
        // 连接本地启动的 ES
        RestClient client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"))
            .build();

        Request searchRequest = new Request("GET", "/test_index/_search");
        searchRequest.setJsonEntity("""
        {
          "query": { "match_all": {} }
        }
        """);
        Response searchResponse = client.performRequest(searchRequest);
        System.out.println(new String(searchResponse.getEntity().getContent().readAllBytes()));
        client.close();
    }

    @Test
    public void bulkInsert() throws IOException {
        RestClient client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"))
            .build();

        Request request = new Request("POST", "/_bulk");
        request.addParameter("refresh", "true"); // 立即刷新，避免查不到新数据
        request.setJsonEntity("""
        { "index" : { "_index" : "test_index", "_id" : "1" } }
        { "name" : "Alice", "age" : 30, "city" : "Singapore", "salary": 10000 }
        { "index" : { "_index" : "test_index", "_id" : "2" } }
        { "name" : "Bob", "age" : 25, "city" : "Tokyo", "salary": 12000 }
        { "index" : { "_index" : "test_index", "_id" : "3" } }
        { "name" : "Charlie", "age" : 28, "city" : "Singapore", "salary": 15000 }
        { "index" : { "_index" : "test_index", "_id" : "4" } }
        { "name" : "David", "age" : 40, "city" : "Tokyo", "salary": 20000 }
        """);

        Response response = client.performRequest(request);
        System.out.println(new String(response.getEntity().getContent().readAllBytes()));
        client.close();
    }

    @Test
    public void queryAll() throws IOException {
        RestClient client = RestClient.builder(new HttpHost("localhost", 9200, "http")).build();

        Request request = new Request("GET", "/test_index/_search");
        request.setJsonEntity("""
        {
          "query": { "match_all": {} },
          "size": 5
        }
        """);

        Response response = client.performRequest(request);
        System.out.println(new String(response.getEntity().getContent().readAllBytes()));
        client.close();
    }

    @Test
    public void aggregateByCity() throws IOException {
        RestClient client = RestClient.builder(new HttpHost("localhost", 9200, "http")).build();

        Request request = new Request("GET", "/test_index/_search");
        request.setJsonEntity("""
        {
          "size": 0,
          "aggs": {
            "avg_salary_by_city": {
              "terms": { "field": "city.keyword" },
              "aggs": {
                "avg_salary": { "avg": { "field": "salary" } }
              }
            }
          }
        }
        """);

        Response response = client.performRequest(request);
        System.out.println(new String(response.getEntity().getContent().readAllBytes()));
        client.close();
    }

}
