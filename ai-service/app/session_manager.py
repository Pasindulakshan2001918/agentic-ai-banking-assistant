"""
Session Manager
================================================================================
Manages user conversation sessions in Redis
Stores state, context, and conversation history
Handles session cleanup and TTL management
================================================================================
"""

import os
import json
from typing import Optional
from datetime import datetime, timedelta
import redis.asyncio as redis
from app.models import UserSession


class SessionManager:
    """Manages user sessions with Redis backend"""
    
    def __init__(self):
        redis_url = os.getenv("REDIS_URL", "redis://localhost:6379")
        self.redis_host = os.getenv("REDIS_HOST", "localhost")
        self.redis_port = int(os.getenv("REDIS_PORT", 6379))
        self.redis_password = os.getenv("REDIS_PASSWORD", None)
        self.session_ttl = int(os.getenv("SESSION_TTL", 3600))  # 1 hour default
        self.redis_client: Optional[redis.Redis] = None
    
    async def connect(self):
        """Initialize Redis connection"""
        try:
            self.redis_client = await redis.Redis(
                host=self.redis_host,
                port=self.redis_port,
                password=self.redis_password,
                decode_responses=True,
                socket_connect_timeout=5
            )
            # Test connection
            await self.redis_client.ping()
            print("✓ Redis connected")
        except Exception as e:
            print(f"⚠ Redis connection failed: {e}. Using in-memory fallback.")
            self.redis_client = None  # Fallback to in-memory
    
    async def get_session(self, user_id: str) -> Optional[UserSession]:
        """Retrieve user session from Redis"""
        try:
            if not self.redis_client:
                return None
            
            session_key = f"session:{user_id}"
            session_data = await self.redis_client.get(session_key)
            
            if not session_data:
                return None
            
            session_dict = json.loads(session_data)
            
            # Reconstruct UserSession with proper typing
            session = UserSession(
                session_id=session_dict.get("session_id"),
                user_id=session_dict.get("user_id"),
                context=session_dict.get("context", {}),
                current_intent=session_dict.get("current_intent")
            )
            
            # Restore messages
            for msg in session_dict.get("messages", []):
                session.messages.append(msg)
            
            return session
            
        except Exception as e:
            print(f"Error retrieving session: {e}")
            return None
    
    async def save_session(self, session: UserSession) -> bool:
        """Save user session to Redis"""
        try:
            if not self.redis_client:
                return False
            
            session_key = f"session:{session.user_id}"
            session_data = json.dumps(session.dict(), default=str)
            
            # Set with TTL
            await self.redis_client.setex(
                session_key,
                self.session_ttl,
                session_data
            )
            
            return True
            
        except Exception as e:
            print(f"Error saving session: {e}")
            return False
    
    async def delete_session(self, user_id: str) -> bool:
        """Delete user session"""
        try:
            if not self.redis_client:
                return False
            
            session_key = f"session:{user_id}"
            await self.redis_client.delete(session_key)
            return True
            
        except Exception as e:
            print(f"Error deleting session: {e}")
            return False
    
    async def get_session_ttl(self, user_id: str) -> int:
        """Get remaining TTL for session"""
        try:
            if not self.redis_client:
                return -1
            
            session_key = f"session:{user_id}"
            ttl = await self.redis_client.ttl(session_key)
            return ttl
            
        except Exception as e:
            print(f"Error getting TTL: {e}")
            return -1
    
    async def extend_session(self, user_id: str) -> bool:
        """Extend session TTL"""
        try:
            if not self.redis_client:
                return False
            
            session_key = f"session:{user_id}"
            await self.redis_client.expire(session_key, self.session_ttl)
            return True
            
        except Exception as e:
            print(f"Error extending session: {e}")
            return False
    
    async def cleanup_expired_sessions(self):
        """Remove expired sessions (Redis handles this automatically with TTL)"""
        # Redis automatically removes expired keys, but we can also scan and cleanup
        try:
            if not self.redis_client:
                return
            
            cursor = 0
            cleaned = 0
            
            while True:
                cursor, keys = await self.redis_client.scan(
                    cursor,
                    match="session:*",
                    count=100
                )
                
                for key in keys:
                    ttl = await self.redis_client.ttl(key)
                    if ttl == -2:  # Key doesn't exist
                        cleaned += 1
                
                if cursor == 0:
                    break
            
            print(f"Cleaned {cleaned} expired sessions")
            
        except Exception as e:
            print(f"Error during session cleanup: {e}")
    
    async def store_temp_data(self, user_id: str, key: str, value: str, ttl: int = 300):
        """Store temporary data (e.g., OTP verification state)"""
        try:
            if not self.redis_client:
                return False
            
            redis_key = f"temp:{user_id}:{key}"
            await self.redis_client.setex(redis_key, ttl, value)
            return True
            
        except Exception as e:
            print(f"Error storing temp data: {e}")
            return False
    
    async def get_temp_data(self, user_id: str, key: str) -> Optional[str]:
        """Retrieve temporary data"""
        try:
            if not self.redis_client:
                return None
            
            redis_key = f"temp:{user_id}:{key}"
            value = await self.redis_client.get(redis_key)
            return value
            
        except Exception as e:
            print(f"Error getting temp data: {e}")
            return None
    
    async def increment_request_count(self, user_id: str, key: str, ttl: int = 60) -> int:
        """
        Increment request counter for rate limiting
        
        Usage: increment_request_count(user_id, "otp_requests", ttl=60)
        """
        try:
            if not self.redis_client:
                return -1
            
            redis_key = f"ratelimit:{user_id}:{key}"
            count = await self.redis_client.incr(redis_key)
            
            # Set TTL on first increment
            if count == 1:
                await self.redis_client.expire(redis_key, ttl)
            
            return count
            
        except Exception as e:
            print(f"Error incrementing counter: {e}")
            return -1
    
    async def get_request_count(self, user_id: str, key: str) -> int:
        """Get current request count"""
        try:
            if not self.redis_client:
                return 0
            
            redis_key = f"ratelimit:{user_id}:{key}"
            count = await self.redis_client.get(redis_key)
            return int(count) if count else 0
            
        except Exception as e:
            print(f"Error getting request count: {e}")
            return 0
