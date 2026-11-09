def handler(event, context):
    tenant_id = event["request"]["userAttributes"].get("custom:tenant_id")
    if tenant_id:
        event["response"]["claimsAndScopeOverrideDetails"] = {
            "accessTokenGeneration": {
                "claimsToAddOrOverride": {"tenant_id": tenant_id}
            }
        }
    
    return event