import type { Meta, StoryObj } from '@storybook/react';
import { StatusBadge } from './StatusBadge';

const meta: Meta<typeof StatusBadge> = {
  title: 'Shared/StatusBadge',
  component: StatusBadge,
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof StatusBadge>;

export const Created: Story = { args: { status: 'CREATED' } };
export const Validated: Story = { args: { status: 'VALIDATED' } };
export const ProvisionalCovered: Story = { args: { status: 'PROVISIONALLY_COVERED' } };
export const SubmittedRealtime: Story = { args: { status: 'SUBMITTED_REALTIME' } };
export const QueuedForBatch: Story = { args: { status: 'QUEUED_FOR_BATCH' } };
export const BatchSubmitted: Story = { args: { status: 'BATCH_SUBMITTED' } };
export const InsurerProcessing: Story = { args: { status: 'INSURER_PROCESSING' } };
export const Confirmed: Story = { args: { status: 'CONFIRMED' } };
export const Rejected: Story = { args: { status: 'REJECTED' } };
export const RetryPending: Story = { args: { status: 'RETRY_PENDING' } };
export const FailedPermanent: Story = { args: { status: 'FAILED_PERMANENT' } };
