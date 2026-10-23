package com.kakuiwong.gaoyangspringai.mcp.server;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @author: gaoyang
 * @Description: MCP Server 凭证校验过滤器，拦截 MCP 端点请求并校验 Bearer Token
 */
public class McpAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(McpAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    private final String expectedToken;

    public McpAuthFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String authHeader = httpRequest.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader)) {
            authHeader = httpRequest.getParameter(AUTH_HEADER);
        }

        // 校验 Authorization 头是否存在且格式正确
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("MCP 请求缺少 Bearer Token, URI: {}", httpRequest.getRequestURI());
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        // 校验 Token 是否匹配
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (!expectedToken.equals(token)) {
            log.warn("MCP 请求 Token 不匹配, URI: {}", httpRequest.getRequestURI());
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.getWriter().write("{\"error\":\"Invalid token\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
