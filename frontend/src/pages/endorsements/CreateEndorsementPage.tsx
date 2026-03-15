import { useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { InlineAlert } from '@/components/shared/InlineAlert';
import { useCreateEndorsement } from '@/hooks/use-endorsements';
import { useEAAccount } from '@/hooks/use-ea-account';
import { useDebounce } from '@/hooks/use-debounce';

const uuidRegex =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const schema = z.object({
  employerId: z.string().regex(uuidRegex, 'Must be a valid UUID'),
  employeeId: z.string().regex(uuidRegex, 'Must be a valid UUID'),
  insurerId: z.string().regex(uuidRegex, 'Must be a valid UUID'),
  policyId: z.string().regex(uuidRegex, 'Must be a valid UUID'),
  type: z.enum(['ADD', 'DELETE', 'UPDATE']),
  coverageStartDate: z.string().min(1, 'Required'),
  coverageEndDate: z.string().optional(),
  employeeName: z.string().min(1, 'Employee name is required'),
  employeeDob: z.string().optional(),
  employeeGender: z.enum(['M', 'F', 'Other']).optional(),
  premiumAmount: z.coerce.number().positive().optional(),
  idempotencyKey: z.string().optional(),
});

type FormData = z.infer<typeof schema>;

export function CreateEndorsementPage() {
  const navigate = useNavigate();
  const createMutation = useCreateEndorsement();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema) as never,
    defaultValues: {
      employerId: '11111111-1111-1111-1111-111111111111',
      insurerId: '22222222-2222-2222-2222-222222222222',
      policyId: '33333333-3333-3333-3333-333333333333',
      type: 'ADD',
    },
  });

  const endorsementType = watch('type');
  const watchedEmployerId = watch('employerId');
  const watchedInsurerId = watch('insurerId');
  const debouncedEmployerId = useDebounce(watchedEmployerId, 600);
  const debouncedInsurerId = useDebounce(watchedInsurerId, 600);

  const isDeleteType = endorsementType === 'DELETE';

  // Check EA balance for inline alert
  const { data: eaAccount } = useEAAccount(debouncedEmployerId, debouncedInsurerId);
  const lowBalance =
    eaAccount && eaAccount.availableBalance < 10000;

  // Auto-scroll to first form error
  useEffect(() => {
    const errorKeys = Object.keys(errors);
    if (errorKeys.length > 0) {
      const firstError = errorKeys[0];
      const el = document.getElementById(firstError);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        el.focus();
      }
    }
  }, [errors]);

  const onSubmit = (data: FormData) => {
    const request = {
      employerId: data.employerId,
      employeeId: data.employeeId,
      insurerId: data.insurerId,
      policyId: data.policyId,
      type: data.type,
      coverageStartDate: data.coverageStartDate,
      coverageEndDate: data.coverageEndDate || undefined,
      employeeData: {
        name: data.employeeName,
        ...(data.employeeDob ? { dob: data.employeeDob } : {}),
        ...(data.employeeGender ? { gender: data.employeeGender } : {}),
      },
      premiumAmount:
        data.premiumAmount !== undefined
          ? Number(data.premiumAmount)
          : undefined,
      idempotencyKey: data.idempotencyKey || undefined,
    };

    createMutation.mutate(request, {
      onSuccess: (endorsement) => {
        navigate(`/endorsements/${endorsement.id}`);
      },
    });
  };

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" asChild aria-label="Back to endorsements list">
          <Link to="/endorsements">
            <ArrowLeft className="h-4 w-4" />
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            Create Endorsement
          </h1>
          <p className="text-muted-foreground text-sm">
            Submit a new endorsement request
          </p>
        </div>
      </div>

      {lowBalance && (
        <InlineAlert
          variant="warning"
          title="Low EA Balance"
          message={`Available balance is ₹${eaAccount.availableBalance.toLocaleString('en-IN')}. This endorsement may fail due to insufficient funds.`}
          dismissible
        />
      )}

      <form id="create-endorsement-form" onSubmit={handleSubmit(onSubmit as never)} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Identifiers</CardTitle>
            <CardDescription>
              Employer, employee, insurer, and policy information
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="employerId">Employer ID</Label>
              <Input id="employerId" {...register('employerId')} />
              {errors.employerId && (
                <p className="text-sm text-red-500">
                  {errors.employerId.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="employeeId">Employee ID</Label>
              <Input
                id="employeeId"
                placeholder="e.g., aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                {...register('employeeId')}
              />
              {errors.employeeId && (
                <p className="text-sm text-red-500">
                  {errors.employeeId.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="insurerId">Insurer ID</Label>
              <Input id="insurerId" {...register('insurerId')} />
              {errors.insurerId && (
                <p className="text-sm text-red-500">
                  {errors.insurerId.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="policyId">Policy ID</Label>
              <Input id="policyId" {...register('policyId')} />
              {errors.policyId && (
                <p className="text-sm text-red-500">
                  {errors.policyId.message}
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Endorsement Details</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label>Type</Label>
              <Select
                value={watch('type')}
                onValueChange={(v) =>
                  setValue('type', v as 'ADD' | 'DELETE' | 'UPDATE')
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ADD">Add</SelectItem>
                  <SelectItem value="DELETE">Delete</SelectItem>
                  <SelectItem value="UPDATE">Update</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {!isDeleteType && (
              <div className="space-y-2">
                <Label htmlFor="premiumAmount">Premium Amount</Label>
                <Input
                  id="premiumAmount"
                  type="number"
                  step="0.01"
                  placeholder="e.g., 1000.00"
                  {...register('premiumAmount')}
                />
              </div>
            )}
            <div className="space-y-2">
              <Label htmlFor="coverageStartDate">Coverage Start Date</Label>
              <Input
                id="coverageStartDate"
                type="date"
                {...register('coverageStartDate')}
              />
              {errors.coverageStartDate && (
                <p className="text-sm text-red-500">
                  {errors.coverageStartDate.message}
                </p>
              )}
            </div>
            {!isDeleteType && (
              <div className="space-y-2">
                <Label htmlFor="coverageEndDate">
                  Coverage End Date{' '}
                  <span className="text-muted-foreground">(Optional)</span>
                </Label>
                <Input
                  id="coverageEndDate"
                  type="date"
                  {...register('coverageEndDate')}
                />
              </div>
            )}
          </CardContent>
        </Card>

        {!isDeleteType && (
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Employee Data</CardTitle>
              <CardDescription>
                Employee information for the insurer
              </CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-3">
              <div className="space-y-2">
                <Label htmlFor="employeeName">Name</Label>
                <Input
                  id="employeeName"
                  placeholder="Full name"
                  {...register('employeeName')}
                />
                {errors.employeeName && (
                  <p className="text-sm text-red-500">
                    {errors.employeeName.message}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="employeeDob">
                  Date of Birth{' '}
                  <span className="text-muted-foreground">(Optional)</span>
                </Label>
                <Input
                  id="employeeDob"
                  type="date"
                  {...register('employeeDob')}
                />
              </div>
              <div className="space-y-2">
                <Label>
                  Gender{' '}
                  <span className="text-muted-foreground">(Optional)</span>
                </Label>
                <Select
                  value={watch('employeeGender') || ''}
                  onValueChange={(v) =>
                    setValue('employeeGender', v as 'M' | 'F' | 'Other')
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select..." />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="M">Male</SelectItem>
                    <SelectItem value="F">Female</SelectItem>
                    <SelectItem value="Other">Other</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </CardContent>
          </Card>
        )}

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Advanced</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <Label htmlFor="idempotencyKey">
                Idempotency Key{' '}
                <span className="text-muted-foreground">
                  (Auto-generated if blank)
                </span>
              </Label>
              <Input
                id="idempotencyKey"
                placeholder="Optional unique key"
                {...register('idempotencyKey')}
              />
            </div>
          </CardContent>
        </Card>

      </form>

      <div className="sticky bottom-0 -mx-4 border-t bg-background px-4 py-3 md:-mx-6 md:px-6">
        <div className="mx-auto flex max-w-2xl justify-end gap-3">
          <Button variant="outline" asChild>
            <Link to="/endorsements">Cancel</Link>
          </Button>
          <Button
            type="submit"
            form="create-endorsement-form"
            disabled={createMutation.isPending}
          >
            {createMutation.isPending
              ? 'Creating...'
              : 'Create Endorsement'}
          </Button>
        </div>
      </div>
    </div>
  );
}
