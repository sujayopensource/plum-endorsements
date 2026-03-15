import { useState } from 'react';
import { format } from 'date-fns';
import { Search, Wallet } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { PageHeader } from '@/components/shared/PageHeader';
import { EmptyState } from '@/components/shared/EmptyState';
import { useEAAccount } from '@/hooks/use-ea-account';

export function EAAccountsPage() {
  const [employerId, setEmployerId] = useState('');
  const [insurerId, setInsurerId] = useState('');
  const [searched, setSearched] = useState(false);

  const { data: account, isLoading } = useEAAccount(
    searched ? employerId : '',
    searched ? insurerId : '',
  );

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearched(true);
  };

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <PageHeader
        title="EA Accounts"
        description="Look up employer account balances and fund reservations"
      />

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Account Lookup</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSearch} className="flex flex-wrap items-end gap-4">
            <div className="flex-1 space-y-2">
              <Label htmlFor="ea-employerId">Employer ID</Label>
              <Input
                id="ea-employerId"
                placeholder="UUID"
                value={employerId}
                onChange={(e) => {
                  setEmployerId(e.target.value);
                  setSearched(false);
                }}
              />
            </div>
            <div className="flex-1 space-y-2">
              <Label htmlFor="ea-insurerId">Insurer ID</Label>
              <Input
                id="ea-insurerId"
                placeholder="UUID"
                value={insurerId}
                onChange={(e) => {
                  setInsurerId(e.target.value);
                  setSearched(false);
                }}
              />
            </div>
            <Button type="submit" disabled={!employerId || !insurerId}>
              <Search className="mr-2 h-4 w-4" />
              Look Up
            </Button>
          </form>
        </CardContent>
      </Card>

      {isLoading && searched && (
        <div className="space-y-4">
          <Skeleton className="h-32" />
        </div>
      )}

      {searched && !isLoading && !account && (
        <EmptyState
          icon={Wallet}
          title="No account found"
          description="No EA account exists for this employer/insurer combination."
        />
      )}

      {account && (
        <div className="grid gap-4 sm:grid-cols-3">
          <Card>
            <CardContent className="pt-6">
              <p className="text-muted-foreground text-sm">Total Balance</p>
              <p className="text-3xl font-bold">
                ₹{account.balance.toLocaleString('en-IN')}
              </p>
            </CardContent>
          </Card>
          <Card className="border-amber-200">
            <CardContent className="pt-6">
              <p className="text-muted-foreground text-sm">Reserved</p>
              <p className="text-3xl font-bold text-amber-600">
                ₹{account.reserved.toLocaleString('en-IN')}
              </p>
            </CardContent>
          </Card>
          <Card className="border-green-200">
            <CardContent className="pt-6">
              <p className="text-muted-foreground text-sm">Available</p>
              <p className="text-3xl font-bold text-green-600">
                ₹{account.availableBalance.toLocaleString('en-IN')}
              </p>
            </CardContent>
          </Card>
        </div>
      )}

      {account && (
        <Card>
          <CardContent className="pt-6">
            <div className="space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">
                  Available vs Total
                </span>
                <span>
                  {((account.availableBalance / account.balance) * 100).toFixed(
                    1,
                  )}
                  %
                </span>
              </div>
              <div className="h-3 overflow-hidden rounded-full bg-gray-100">
                <div
                  className="h-full rounded-full bg-green-500 transition-all"
                  style={{
                    width: `${(account.availableBalance / account.balance) * 100}%`,
                  }}
                />
              </div>
              <p className="text-muted-foreground text-xs">
                Last updated:{' '}
                {format(new Date(account.updatedAt), 'dd MMM yyyy HH:mm:ss')}
              </p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
