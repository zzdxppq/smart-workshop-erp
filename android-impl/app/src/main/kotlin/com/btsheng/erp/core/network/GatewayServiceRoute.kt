package com.btsheng.erp.core.network

/** Gateway：/erp-{service}/auth/login */
object GatewayServiceRoute {

    private val servicePrefix = Regex("^/erp-(platform|business|production)(/|$)")
    private val productRoutes = Regex("^/products/[^/]+/routes(/|$)")

    private val platformPrefixes = listOf(
        "/auth", "/users", "/roles", "/audit", "/print", "/label-templates", "/printers",
        "/changelogs", "/thresholds", "/params", "/dicts", "/platform", "/admin/workflows",
        "/workflows", "/approvals", "/files", "/app/login", "/app/sync", "/app/messages",
        "/app/scan/route",
    )

    private val productionPrefixes = listOf(
        "/workorders", "/production", "/outsource-eta", "/outsource-quality",
        "/outsource-incoming", "/outsource-states", "/outsource-switches",
        "/app/production", "/app/workorders", "/app/transfer", "/reworks", "/mrp",
        "/machines", "/processes", "/allocations", "/workorders",
    )

    private val businessPrefixes = listOf(
        "/incoming", "/incoming-alert", "/reconciles", "/reconcile", "/purchase",
        "/rfq", "/po", "/vendors", "/orders", "/quotes", "/materials", "/material-barcode",
        "/approval", "/reports", "/internal", "/warehouse",
        "/dashboard", "/quality", "/drawings", "/quality-inspection", "/hr",
    )

    fun resolveServiceId(path: String): String {
        val p = path.substringBefore('?')
        if (p.startsWith("/ws") || p.startsWith("/sse")) return "erp-business"
        if (p.startsWith("/outsource-cost")) return "erp-business"
        if (p.startsWith("/app/scan") && !p.startsWith("/app/scan/route")) return "erp-business"
        if (productRoutes.containsMatchIn(p)) return "erp-production"
        if (p.startsWith("/outsource")) return "erp-production"
        if (productionPrefixes.any { p == it || p.startsWith("$it/") }) return "erp-production"
        if (businessPrefixes.any { p == it || p.startsWith("$it/") }) return "erp-business"
        if (platformPrefixes.any { p == it || p.startsWith("$it/") }) return "erp-platform"
        return "erp-business"
    }

    fun resolveGatewayPath(encodedPath: String): String {
        if (servicePrefix.containsMatchIn(encodedPath)) return encodedPath
        val logical = when {
            encodedPath.startsWith("/") -> encodedPath
            encodedPath.startsWith("app/") -> "/$encodedPath"
            else -> return encodedPath
        }
        val service = resolveServiceId(logical)
        return "/$service$logical"
    }
}
