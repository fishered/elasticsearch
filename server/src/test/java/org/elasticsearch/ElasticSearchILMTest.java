/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch;

import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.internal.ElasticsearchClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.repositories.blobstore.ESMockAPIBasedRepositoryIntegTestCase;
import org.elasticsearch.test.rest.ESRestTestCase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * @author fisher
 */
public class ElasticSearchILMTest {

    private static final Logger logger = LogManager.getLogger(ElasticSearchILMTest.class);

    @Test
    public void testRoll() throws IOException {
        // 连接本地启动的 ES
        RestClient client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"))
            .build();

        // === 1. 创建带 write alias 的初始索引
//        {
//            String body = """
//            {
//              "aliases": {
//                "mylogs-write": {
//                  "is_write_index": true
//                }
//              }
//            }
//            """;
//
//            Request req = new Request("PUT", "/mylogs-000001");
//            req.setJsonEntity(body);
//            client.performRequest(req);
//        }

        // === 2. 索引 500 条文档（模拟写入）
        for (int i = 0; i < 500; i++) {
            long ts = System.currentTimeMillis();
            String json = "{ \"msg\": \"log-" + i + "\", \"ts\": " + ts + " }";

            Request req = new Request("POST", "/mylogs-write/_doc");
            req.setJsonEntity(json);
            client.performRequest(req);

            if (i % 100 == 0) {
                logger.info("Indexed {} docs", i);
            }
        }

        // === 3. 触发 Rollover（条件：max_docs = 200）
        String rolloverCond = """
        {
          "conditions": {
            "max_docs": 200
          }
        }
        """;

        Request rollReq = new Request("POST", "/mylogs-write/_rollover");
        rollReq.setJsonEntity(rolloverCond);

        Response rollResp = client.performRequest(rollReq);
        String rollBody = EntityUtils.toString(rollResp.getEntity());
        logger.info("Rollover response: {}", rollBody);

        // 验证是否 rollover 成功
        assertTrue("Should trigger rollover", rollBody.contains("\"rolled_over\":true"));
        assertTrue(rollBody.contains("mylogs-000002"));

        // 写入新索引
        Request req = new Request("POST", "/mylogs-write/_doc");
        req.setJsonEntity("{\"msg\":\"after-rollover\"}");
        Response writeResp = client.performRequest(req);
        String writeBody = EntityUtils.toString(writeResp.getEntity());
        logger.info("Write after rollover: {}", writeBody);
        assertTrue(writeBody.contains("mylogs-000002"));
    }

    @Test
    public void see() throws IOException {
        RestClient client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"))
            .build();

        // === 5. 查看集群健康 / 索引 / 分片
        Request healthReq = new Request("GET", "/_cluster/health");
        Response healthResp = client.performRequest(healthReq);
        System.out.println("Cluster Health: " + EntityUtils.toString(healthResp.getEntity()));

        Request indicesReq = new Request("GET", "/_cat/indices?v&h=index,docs.count,store.size");
        Response indicesResp = client.performRequest(indicesReq);
        System.out.println("Indices Info:\n" + EntityUtils.toString(indicesResp.getEntity()));

        Request shardsReq = new Request("GET", "/_cat/shards?v&h=index,shard,prirep,state,store");
        Response shardsResp = client.performRequest(shardsReq);
        System.out.println("Shards Info:\n" + EntityUtils.toString(shardsResp.getEntity()));

        // === 6. 查询文档
        Request searchReq = new Request("GET", "/mylogs-write/_search?q=msg:log-1");
        Response searchResp = client.performRequest(searchReq);
        System.out.println("Search result: " + EntityUtils.toString(searchResp.getEntity()));
    }

    @Test
    public void mergeQuery() throws IOException {
        RestClient client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"))
            .build();

        try {
            // 1️⃣ 查询所有索引和文档数、大小
            Request indicesReq = new Request("GET", "/_cat/indices/mylogs-*?v&s=index");
            Response indicesResp = client.performRequest(indicesReq);
            String indicesBody = EntityUtils.toString(indicesResp.getEntity());
            System.out.println("=== Indices Info ===");
            System.out.println(indicesBody);

            // 2️⃣ 查询所有分片状态
            Request shardsReq = new Request("GET", "/_cat/shards/mylogs-*?v&s=index");
            Response shardsResp = client.performRequest(shardsReq);
            String shardsBody = EntityUtils.toString(shardsResp.getEntity());
            System.out.println("=== Shards Info ===");
            System.out.println(shardsBody);

            // 3️⃣ 查询所有索引文档
            Request searchReq = new Request("GET", "/mylogs-*/_search");
            searchReq.addParameter("size", "1000"); // 获取前1000条
            searchReq.addParameter("pretty", "true");

            Response searchResp = client.performRequest(searchReq);
            String searchBody = EntityUtils.toString(searchResp.getEntity());
            System.out.println("=== Search Result ===");
            System.out.println(searchBody);

        } finally {
            client.close();
        }
    }

    @Test
    public void status() throws IOException {
        RestClient client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"))
            .build();

        Request req = new Request("GET", "/_nodes");
        Response resp = client.performRequest(req);
        String json = EntityUtils.toString(resp.getEntity());
        System.out.println(json);
    }

}
