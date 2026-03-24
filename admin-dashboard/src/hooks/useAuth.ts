'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import type { User } from 'firebase/auth';
import { onAuthStateChanged, checkIsAdmin } from '@/lib/auth';

interface AuthState {
  user: User | null;
  loading: boolean;
  isAdmin: boolean;
}

export function useAuth(requireAuth = true): AuthState {
  const [state, setState] = useState<AuthState>({ user: null, loading: true, isAdmin: false });
  const router = useRouter();

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(async (user) => {
      if (user) {
        const admin = await checkIsAdmin(user.uid);
        setState({ user, loading: false, isAdmin: admin });
        if (!admin && requireAuth) {
          router.replace('/login');
        }
      } else {
        setState({ user: null, loading: false, isAdmin: false });
        if (requireAuth) {
          router.replace('/login');
        }
      }
    });
    return () => unsubscribe();
  }, [requireAuth, router]);

  return state;
}
