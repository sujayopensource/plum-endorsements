import { useSearchParams } from 'react-router-dom';
import { useCallback } from 'react';

export function useUrlFilters() {
  const [searchParams, setSearchParams] = useSearchParams();

  const getParam = useCallback(
    (key: string, defaultValue = '') => searchParams.get(key) ?? defaultValue,
    [searchParams],
  );

  const setParam = useCallback(
    (key: string, value: string) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        if (value) {
          next.set(key, value);
        } else {
          next.delete(key);
        }
        return next;
      });
    },
    [setSearchParams],
  );

  const getArrayParam = useCallback(
    (key: string) => searchParams.getAll(key),
    [searchParams],
  );

  const setArrayParam = useCallback(
    (key: string, values: string[]) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        next.delete(key);
        values.forEach((v) => next.append(key, v));
        return next;
      });
    },
    [setSearchParams],
  );

  const setParams = useCallback(
    (updates: Record<string, string | string[] | undefined>) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        for (const [key, value] of Object.entries(updates)) {
          next.delete(key);
          if (value === undefined) continue;
          if (Array.isArray(value)) {
            value.forEach((v) => next.append(key, v));
          } else if (value) {
            next.set(key, value);
          }
        }
        return next;
      });
    },
    [setSearchParams],
  );

  return { searchParams, getParam, setParam, getArrayParam, setArrayParam, setParams };
}
