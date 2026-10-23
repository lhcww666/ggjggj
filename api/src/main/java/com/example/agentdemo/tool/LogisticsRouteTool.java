package com.example.agentdemo.tool;

import com.example.agentdemo.context.AgentContext;
import com.example.integrations.sf.SfExpressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LogisticsRouteTool implements Tool {

    private final SfExpressService sfExpressService;
    private final ObjectMapper mapper;

    public LogisticsRouteTool(SfExpressService sfExpressService, ObjectMapper mapper) {
        this.sfExpressService = sfExpressService;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "query_logistics_route";
    }

    @Override
    public String description() {
        return "按顺丰运单号查询物流轨迹。只接受以 SF 开头的 waybillNo，不接受发货单号。";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("waybillNo").put("type", "string");
        schema.putArray("required").add("waybillNo");
        return schema;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(AgentContext context, ObjectNode input) {
        String waybillNo = input.path("waybillNo").asText("").trim().toUpperCase(Locale.ROOT);
        if (waybillNo.isEmpty()) {
            ObjectNode data = mapper.createObjectNode();
            data.put("code", "WAYBILL_REQUIRED");
            return ToolResult.error("请提供顺丰运单号", data);
        }

        Map<String, Object> route = sfExpressService.route(waybillNo);
        ObjectNode data = mapper.valueToTree(route);
        if (!Boolean.TRUE.equals(route.get("success"))) {
            return ToolResult.error(String.valueOf(route.get("message")), data);
        }

        List<Map<String, Object>> routes = (List<Map<String, Object>>) route.get("routes");
        if (routes == null || routes.isEmpty()) {
            data.put("code", "ROUTE_NOT_FOUND");
            return ToolResult.error("运单 " + waybillNo + " 暂无物流轨迹", data);
        }
        Map<String, Object> latest = routes.get(routes.size() - 1);
        String answer = "运单 " + waybillNo + " 共 " + routes.size()
                + " 条轨迹，最新状态：" + latest.get("status") + "（" + latest.get("address") + "）";
        return ToolResult.ok(answer, data);
    }
}
