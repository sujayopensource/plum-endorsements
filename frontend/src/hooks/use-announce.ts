import { useCallback } from 'react';

export function useAnnounce() {
  const announce = useCallback((message: string) => {
    const el = document.getElementById('live-region');
    if (el) {
      el.textContent = message;
      setTimeout(() => {
        el.textContent = '';
      }, 3000);
    }
  }, []);

  return { announce };
}
