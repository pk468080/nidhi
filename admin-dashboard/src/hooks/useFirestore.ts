'use client';

import { useState, useEffect, useCallback } from 'react';
import {
  collection,
  query,
  onSnapshot,
  QueryConstraint,
  DocumentData,
  QuerySnapshot,
  getDocs,
  doc,
  getDoc,
  addDoc,
  updateDoc,
  deleteDoc,
  serverTimestamp,
} from 'firebase/firestore';
import { db } from '@/lib/firebase';

interface UseCollectionResult<T> {
  data: T[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useCollection<T extends { id: string }>(
  collectionName: string,
  constraints: QueryConstraint[] = []
): UseCollectionResult<T> {
  const [data, setData] = useState<T[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const refetch = useCallback(() => setRefreshKey((k) => k + 1), []);

  useEffect(() => {
    const col = collection(db, collectionName);
    const q = query(col, ...constraints);
    setLoading(true);

    const unsubscribe = onSnapshot(
      q,
      (snapshot: QuerySnapshot<DocumentData>) => {
        const docs = snapshot.docs.map((d) => ({ id: d.id, ...d.data() } as T));
        setData(docs);
        setLoading(false);
        setError(null);
      },
      (err) => {
        setError(err.message);
        setLoading(false);
      }
    );
    return () => unsubscribe();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [collectionName, refreshKey]);

  return { data, loading, error, refetch };
}

interface UseDocumentResult<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

export function useDocument<T extends { id: string }>(
  collectionName: string,
  docId: string | null
): UseDocumentResult<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!docId) {
      setLoading(false);
      return;
    }
    const docRef = doc(db, collectionName, docId);
    const unsubscribe = onSnapshot(
      docRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setData({ id: snapshot.id, ...snapshot.data() } as T);
        } else {
          setData(null);
        }
        setLoading(false);
      },
      (err) => {
        setError(err.message);
        setLoading(false);
      }
    );
    return () => unsubscribe();
  }, [collectionName, docId]);

  return { data, loading, error };
}

// CRUD helpers
export function useFirestoreCRUD(collectionName: string) {
  const [loading, setLoading] = useState(false);

  const fetchAll = useCallback(async <T extends { id: string }>(
    constraints: QueryConstraint[] = []
  ): Promise<T[]> => {
    setLoading(true);
    const col = collection(db, collectionName);
    const q = query(col, ...constraints);
    const snapshot = await getDocs(q);
    setLoading(false);
    return snapshot.docs.map((d) => ({ id: d.id, ...d.data() } as T));
  }, [collectionName]);

  const fetchOne = useCallback(async <T extends { id: string }>(docId: string): Promise<T | null> => {
    const docRef = doc(db, collectionName, docId);
    const snapshot = await getDoc(docRef);
    return snapshot.exists() ? ({ id: snapshot.id, ...snapshot.data() } as T) : null;
  }, [collectionName]);

  const create = useCallback(async <T extends Record<string, unknown>>(data: T): Promise<string> => {
    const col = collection(db, collectionName);
    const docRef = await addDoc(col, { ...data, createdAt: serverTimestamp() });
    return docRef.id;
  }, [collectionName]);

  const update = useCallback(async <T extends Record<string, unknown>>(docId: string, data: Partial<T>): Promise<void> => {
    const docRef = doc(db, collectionName, docId);
    await updateDoc(docRef, { ...data, updatedAt: serverTimestamp() });
  }, [collectionName]);

  const remove = useCallback(async (docId: string): Promise<void> => {
    const docRef = doc(db, collectionName, docId);
    await deleteDoc(docRef);
  }, [collectionName]);

  return { fetchAll, fetchOne, create, update, remove, loading };
}
