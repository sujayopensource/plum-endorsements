import { createBrowserRouter } from 'react-router-dom';
import { RootLayout } from './layout';
import { DashboardPage } from '@/pages/dashboard/DashboardPage';
import { EndorsementsListPage } from '@/pages/endorsements/EndorsementsListPage';
import { EndorsementDetailPage } from '@/pages/endorsements/EndorsementDetailPage';
import { CreateEndorsementPage } from '@/pages/endorsements/CreateEndorsementPage';
import { BatchProgressPage } from '@/pages/endorsements/BatchProgressPage';
import { EAAccountsPage } from '@/pages/ea-accounts/EAAccountsPage';
import { InsurersPage } from '@/pages/insurers/InsurersPage';
import { InsurerDetailPage } from '@/pages/insurers/InsurerDetailPage';
import { ReconciliationPage } from '@/pages/reconciliation/ReconciliationPage';
import { IntelligenceDashboardPage } from '@/pages/intelligence/IntelligenceDashboardPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      {
        path: 'endorsements',
        children: [
          { index: true, element: <EndorsementsListPage /> },
          { path: 'new', element: <CreateEndorsementPage /> },
          { path: 'batches', element: <BatchProgressPage /> },
          { path: ':id', element: <EndorsementDetailPage /> },
        ],
      },
      { path: 'ea-accounts', element: <EAAccountsPage /> },
      {
        path: 'insurers',
        children: [
          { index: true, element: <InsurersPage /> },
          { path: ':insurerId', element: <InsurerDetailPage /> },
        ],
      },
      { path: 'reconciliation', element: <ReconciliationPage /> },
      { path: 'intelligence', element: <IntelligenceDashboardPage /> },
    ],
  },
]);
