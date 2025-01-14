package com.personal.apigateway.filter;

import com.personal.apigateway.model.RateLimiterResult;
import com.personal.apigateway.service.LoadBalancer;
import com.personal.apigateway.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.stream.Collectors;

public class GatewayFilter extends GenericFilterBean {

    private final int TOO_MANY_REQUESTS = 406;
    private final RateLimiterService rateLimiterService;
    private final LoadBalancer loadBalancer;
    private final WebClient.Builder webClientbuilder;


    @Autowired
    public GatewayFilter(RateLimiterService rateLimiterService,
                         LoadBalancer loadBalancer, WebClient.Builder webClientbuilder){
        this.rateLimiterService = rateLimiterService;
        this.loadBalancer = loadBalancer;
        this.webClientbuilder = webClientbuilder;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        if (requestURI.startsWith("/gateway")) {
            // Getting clientID (IP or optional key)
            String clientIp = getClientIdentifier(httpRequest);

            // Apply rate limiting
            RateLimiterResult result = rateLimiterService.allowRequest(clientIp);

            //adding rate limited related headers
            addRateLimitHeaders(httpResponse, result);

            if (!result.isAllowed()) {
                httpResponse.setStatus(TOO_MANY_REQUESTS);
                httpResponse.getWriter().write("Rate limit exceeded!");
                String errorMessage = String.format("""
                        {
                            error:"Rate limit exceeded",
                            "maxRequests":%d,
                            "retryAfter":%d,
                            
                        }""",
                        result.maxRequests(),
                        result.windowSeconds()
                );
                httpResponse.getWriter().write(errorMessage);
                return;
            }
            // Strip the /gateway prefix and forward the request
            String actualPath = requestURI.replaceFirst("/gateway", "");
            try{

                String serverUrl = loadBalancer.getNextServerUrl();
                String targetUrl = serverUrl + actualPath;

                WebClient webClient = webClientbuilder.build();
                WebClient.RequestBodySpec
                        requestBodySpec = webClient.method(HttpMethod.valueOf(httpRequest.getMethod()))
                        .uri(targetUrl);


                //Copy all the headers from original request
                Enumeration<String> headerNames = httpRequest.getHeaderNames();
                while(headerNames.hasMoreElements()){
                    String header = headerNames.nextElement();
                    String headerValue = httpRequest.getHeader(header);
                    requestBodySpec.header(header, headerValue);
                }

                //Copy query params
                String queryString = httpRequest.getQueryString();
                if(StringUtils.hasText(queryString)){
                    targetUrl += "?" + queryString;
                }

                //Reading request body
                String requestBody = null;
                if (httpRequest.getMethod().equals(HttpMethod.POST.name())
                        || httpRequest.getMethod().equals(HttpMethod.PUT.name())) {
                    requestBody = new BufferedReader(new InputStreamReader(httpRequest.getInputStream()))
                            .lines()
                            .collect(Collectors.joining("\n"));
                }

                // Execute the request
                ResponseEntity<byte[]> proxyResponse = requestBodySpec
                        .bodyValue(requestBody != null ? requestBody : "")
                        .retrieve()
                        .toEntity(byte[].class)
                        .block();  // blocking call for synchronous response


                //Setting response headers
                proxyResponse.getHeaders().forEach((name, values)->{
                    values.forEach(value -> httpResponse.setHeader(name, value));
                });

                //setting response status code
                httpResponse.setStatus(proxyResponse.getStatusCode().value());

                //Setting response body
                if(proxyResponse.getBody() != null){
                    httpResponse.getOutputStream().write(proxyResponse.getBody());
                }
            }catch (Exception e){

            }

//            chain.doFilter(new CustomHttpServletRequestWrapper(httpRequest, actualPath), response);
//
//            System.out.println(response.getOutputStream());
            return;
        }

        // If the request is not to /gateway, proceed as normal
        System.out.println("without gateway");
        chain.doFilter(request, response);
    }

    
    /* Custom HttpServletRequestWrapper to modify the request URI
        - post checking the url it redirects to original URI
     */
    private static class CustomHttpServletRequestWrapper extends HttpServletRequestWrapper {
        private final String newRequestURI;
        public CustomHttpServletRequestWrapper(HttpServletRequest request, String newRequestURI) {
            super(request);
            this.newRequestURI = newRequestURI;
        }

        @Override
        public String getRequestURI() {
            return newRequestURI;
        }

        @Override
        public String getServletPath() {
            return newRequestURI;
        }
    }

    private void addRateLimitHeaders(HttpServletResponse response, RateLimiterResult result) {
        response.addHeader("X-RateLimit-Limit", String.valueOf(result.maxRequests()));
        response.addHeader("X-RateLimit-Reset", String.valueOf(result.windowSeconds()));
    }

    private String getClientIdentifier(HttpServletRequest request) {
        InetAddressValidator validator = null;
        String clientIp = null;
        String xRealIp = request.getHeader("X-Real-IP");
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        // used by the majority of load balancers
        //considering the proxy servers are trushtworthy and client ip address is not a spoof one
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String[] xForwardedForIps = xForwardedFor.split(",");
            clientIp = xForwardedForIps != null && xForwardedForIps.length > 0
                    ? xForwardedForIps[0].trim() : xForwardedFor;
        }

        // used by Nginx
        if (clientIp == null && xRealIp != null && StringUtils.hasText(xRealIp))
            clientIp = xRealIp.trim();

        // otherwise uses the remote IP address obtained by our Servlet container
        if (clientIp == null)
            clientIp = request.getRemoteAddr();

        //validating IP address
        validator = InetAddressValidator.getInstance();
        if (clientIp != null) {
            if (validator.isValid(clientIp)) {
                clientIp = clientIp.replaceAll(":", "_");
            } else {
                clientIp = clientIp.replaceAll(":", "_");
                clientIp += "_notValid";
            }
        } else
            clientIp = "unknown_IP";

        return clientIp;
    }
}
