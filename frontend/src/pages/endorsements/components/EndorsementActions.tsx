import { useState } from 'react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Send, CheckCircle, XCircle, AlertTriangle, Info } from 'lucide-react';
import {
  useSubmitEndorsement,
  useConfirmEndorsement,
  useRejectEndorsement,
} from '@/hooks/use-endorsements';
import type { EndorsementStatus } from '@/types/endorsement';

interface EndorsementActionsProps {
  id: string;
  status: EndorsementStatus;
  retryCount: number;
}

export function EndorsementActions({
  id,
  status,
  retryCount,
}: EndorsementActionsProps) {
  const submitMutation = useSubmitEndorsement();
  const confirmMutation = useConfirmEndorsement();
  const rejectMutation = useRejectEndorsement();

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [submitOpen, setSubmitOpen] = useState(false);
  const [insurerReference, setInsurerReference] = useState('');
  const [rejectReason, setRejectReason] = useState('');

  const handleSubmit = () => {
    submitMutation.mutate(id, { onSuccess: () => setSubmitOpen(false) });
  };

  const handleConfirm = () => {
    if (!insurerReference.trim()) return;
    confirmMutation.mutate(
      { id, insurerReference: insurerReference.trim() },
      {
        onSuccess: () => {
          setConfirmOpen(false);
          setInsurerReference('');
        },
      },
    );
  };

  const handleReject = () => {
    if (!rejectReason.trim()) return;
    rejectMutation.mutate(
      { id, reason: rejectReason.trim() },
      {
        onSuccess: () => {
          setRejectOpen(false);
          setRejectReason('');
        },
      },
    );
  };

  if (status === 'CONFIRMED' || status === 'FAILED_PERMANENT') {
    return (
      <div className="rounded-lg border bg-muted/50 p-4">
        <div className="flex items-center gap-2">
          <Info className="h-4 w-4 text-muted-foreground" />
          <span className="text-sm text-muted-foreground">
            This endorsement has reached a terminal state. No further actions
            are available.
          </span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <h3 className="text-sm font-semibold">Actions</h3>

      {(status === 'PROVISIONALLY_COVERED' || status === 'RETRY_PENDING') && (
        <Dialog open={submitOpen} onOpenChange={setSubmitOpen}>
          <DialogTrigger asChild>
            <Button className="w-full gap-2">
              <Send className="h-4 w-4" />
              Submit to Insurer
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Submit to Insurer</DialogTitle>
              <DialogDescription>
                This will submit the endorsement to the insurer for processing
                via the real-time path.
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="outline" onClick={() => setSubmitOpen(false)}>
                Cancel
              </Button>
              <Button
                onClick={handleSubmit}
                disabled={submitMutation.isPending}
              >
                {submitMutation.isPending ? 'Submitting...' : 'Submit'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}

      {(status === 'SUBMITTED_REALTIME' ||
        status === 'BATCH_SUBMITTED' ||
        status === 'INSURER_PROCESSING') && (
        <>
          <Dialog open={confirmOpen} onOpenChange={setConfirmOpen}>
            <DialogTrigger asChild>
              <Button
                variant="outline"
                className="w-full gap-2 border-green-300 text-green-700 hover:bg-green-50"
              >
                <CheckCircle className="h-4 w-4" />
                Confirm
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Confirm Endorsement</DialogTitle>
                <DialogDescription>
                  Enter the insurer reference number to confirm this
                  endorsement.
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-2">
                <Label htmlFor="insurerRef">Insurer Reference</Label>
                <Input
                  id="insurerRef"
                  placeholder="e.g., INS-REF-2026-001"
                  value={insurerReference}
                  onChange={(e) => setInsurerReference(e.target.value)}
                />
              </div>
              <DialogFooter>
                <Button
                  variant="outline"
                  onClick={() => setConfirmOpen(false)}
                >
                  Cancel
                </Button>
                <Button
                  onClick={handleConfirm}
                  disabled={
                    !insurerReference.trim() || confirmMutation.isPending
                  }
                  className="bg-green-600 hover:bg-green-700"
                >
                  {confirmMutation.isPending ? 'Confirming...' : 'Confirm'}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>

          <Dialog open={rejectOpen} onOpenChange={setRejectOpen}>
            <DialogTrigger asChild>
              <Button variant="destructive" className="w-full gap-2">
                <XCircle className="h-4 w-4" />
                Reject
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Reject Endorsement</DialogTitle>
                <DialogDescription>
                  {retryCount < 3
                    ? `This endorsement will be set to Retry Pending (attempt ${retryCount + 1}/3).`
                    : 'Max retries exhausted. This will permanently fail the endorsement.'}
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-2">
                <Label htmlFor="reason">Rejection Reason</Label>
                <Textarea
                  id="reason"
                  placeholder="Reason for rejection..."
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                />
              </div>
              {retryCount >= 2 && (
                <div className="flex items-center gap-2 rounded-md border border-amber-200 bg-amber-50 p-3">
                  <AlertTriangle className="h-4 w-4 text-amber-600" />
                  <span className="text-sm text-amber-800">
                    This is the final retry. Rejection will permanently fail
                    this endorsement.
                  </span>
                </div>
              )}
              <DialogFooter>
                <Button variant="outline" onClick={() => setRejectOpen(false)}>
                  Cancel
                </Button>
                <Button
                  variant="destructive"
                  onClick={handleReject}
                  disabled={!rejectReason.trim() || rejectMutation.isPending}
                >
                  {rejectMutation.isPending ? 'Rejecting...' : 'Reject'}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </>
      )}

      {status === 'REJECTED' && retryCount < 3 && (
        <Dialog open={submitOpen} onOpenChange={setSubmitOpen}>
          <DialogTrigger asChild>
            <Button
              variant="outline"
              className="w-full gap-2 border-amber-300 text-amber-700 hover:bg-amber-50"
            >
              <Send className="h-4 w-4" />
              Retry Submission
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Retry Submission</DialogTitle>
              <DialogDescription>
                Retry attempt {retryCount + 1} of 3. This will resubmit the
                endorsement to the insurer.
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="outline" onClick={() => setSubmitOpen(false)}>
                Cancel
              </Button>
              <Button
                onClick={handleSubmit}
                disabled={submitMutation.isPending}
              >
                {submitMutation.isPending ? 'Submitting...' : 'Retry'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}
