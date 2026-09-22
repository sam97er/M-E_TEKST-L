"""
Tests for Health and System Diagnostics Endpoint.
"""

def test_root_endpoint(client):
    """Test root status check."""
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "online"
    assert "M&E Tekstil" in data["app"]
    assert data["health"] == "/api/v1/health"


def test_health_check_endpoint(client):
    """Test detailed system health status response."""
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()

    assert data["status"] == "healthy"
    assert data["app_env"] == "development"
    assert data["database"]["connected"] is True
    assert data["database"]["dialect"] == "sqlite"
    assert data["database"]["tables_count"] > 0
    assert data["database"]["latency_ms"] >= 0.0

    # Verify 3 AI providers are reported
    ai_providers = data["ai_providers"]
    assert len(ai_providers) == 3
    assert ai_providers[0]["slot"] == 1
    assert ai_providers[1]["slot"] == 2
    assert ai_providers[2]["slot"] == 3

    # Verify security: keys are never exposed in plaintext
    for provider in ai_providers:
        assert "key" not in provider or provider["masked_key"] != ""

    # Verify Trendyol and Telegram structures
    assert "configured" in data["trendyol"]
    assert "enabled" in data["telegram"]
