"""Run the FastMCP server (stdio for Claude Desktop, SSE for web).
Usage:
  python mcp_server.py               # stdio (Claude Desktop)
  MCP_TRANSPORT=sse python mcp_server.py   # SSE
"""
from __future__ import annotations

from agent.config import MCP_TRANSPORT, MCP_HOST, MCP_PORT
from agent.tools import mcp

if __name__ == "__main__":
    if MCP_TRANSPORT == "sse":
        mcp.run(transport="sse", host=MCP_HOST, port=MCP_PORT)
    else:
        mcp.run(transport="stdio")
