package wfederico.backendjavacoretemplate.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import wfederico.backendjavacoretemplate.core.web.ApiResponseBase;

import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SecurityFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SecurityFilter (StringRedisTemplate redisTemplate, ObjectMapper objectMapper){
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException{

        //Retrieve client IP
        String clientIp = request.getRemoteAddr();
        String redisKey = "rate_limit:" + clientIp;
        String traceId = MDC.get("traceId");
        String blackListKey = "blacklist:" + clientIp;

        Boolean isBlacklisted = redisTemplate.hasKey(blackListKey); //Check if the ip is blacklisted

        if(isBlacklisted){
            response.setStatus(403);
            response.setContentType("application/json");

            return;
        }

        //Incr the ip counter in redis
        Long requestCount = redisTemplate.opsForValue().increment(redisKey);

        //Assign ttl for first time request
        if( requestCount != null && requestCount == 1){
            redisTemplate.expire(redisKey, 60, TimeUnit.SECONDS);
        }

        

        //Check if the rate limit is reached
        if (requestCount != null && requestCount > 10) {
            response.setStatus(429);
            response.setContentType("application/json");

            ApiResponseBase<Void> apiResponse = ApiResponseBase.<Void>builder()
                .status(249)
                .message("Too many requests")
                .traceId(traceId)
                .build();
            
            //Parse value as json response
            String jsonResponse = objectMapper.writeValueAsString(apiResponse);

            //Write the json directly into the HTTP response
            response.getWriter().write(jsonResponse);
            return;
        }

        //all check passed, lets the request go on
        filterChain.doFilter(request,response);
    }
}
