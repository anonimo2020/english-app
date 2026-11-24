"""
Caché TTL ligero en memoria con invalidación selectiva y global.
"""

from __future__ import annotations

import threading
import time
from typing import Any


class TTLCache:
    def __init__(self, default_ttl_sec: int = 120, max_entries: int = 256) -> None:
        self.default_ttl_sec = default_ttl_sec
        self.max_entries = max_entries
        self._store: dict[str, tuple[float, Any]] = {}
        self._lock = threading.Lock()
        self.hits = 0
        self.misses = 0

    def get(self, key: str) -> Any | None:
        now = time.monotonic()
        with self._lock:
            entry = self._store.get(key)
            if not entry:
                self.misses += 1
                return None
            expires_at, val = entry
            if now >= expires_at:
                self._store.pop(key, None)
                self.misses += 1
                return None
            self.hits += 1
            return val

    def set(self, key: str, value: Any, ttl_sec: int | None = None) -> None:
        ttl = ttl_sec if ttl_sec is not None else self.default_ttl_sec
        now = time.monotonic()
        with self._lock:
            if len(self._store) >= self.max_entries:
                # Expira entradas caducadas primero, o elimina la más antigua
                expired = [k for k, (exp, _) in self._store.items() if now >= exp]
                for k in expired:
                    self._store.pop(k, None)
                if len(self._store) >= self.max_entries:
                    oldest = min(self._store.items(), key=lambda kv: kv[1][0])[0]
                    self._store.pop(oldest, None)
            self._store[key] = (now + ttl, value)

    def invalidate(self, key: str) -> None:
        with self._lock:
            self._store.pop(key, None)

    def invalidate_all(self) -> None:
        with self._lock:
            self._store.clear()

    def stats(self) -> dict[str, int]:
        with self._lock:
            return {
                "entries": len(self._store),
                "hits": self.hits,
                "misses": self.misses,
            }


edu_cache = TTLCache(default_ttl_sec=90, max_entries=256)
