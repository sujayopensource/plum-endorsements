import type { Meta, StoryObj } from '@storybook/react';
import { StatusTimeline } from './StatusTimeline';

const meta: Meta<typeof StatusTimeline> = {
  title: 'Endorsements/StatusTimeline',
  component: StatusTimeline,
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof StatusTimeline>;

export const RealtimeCreated: Story = {
  args: {
    status: 'CREATED',
    batchId: null,
    retryCount: 0,
    failureReason: null,
  },
};

export const RealtimeProvisionalCovered: Story = {
  args: {
    status: 'PROVISIONALLY_COVERED',
    batchId: null,
    retryCount: 0,
    failureReason: null,
  },
};

export const RealtimeConfirmed: Story = {
  args: {
    status: 'CONFIRMED',
    batchId: null,
    retryCount: 0,
    failureReason: null,
  },
};

export const BatchPath: Story = {
  args: {
    status: 'BATCH_SUBMITTED',
    batchId: 'batch-001',
    retryCount: 0,
    failureReason: null,
  },
};

export const Rejected: Story = {
  args: {
    status: 'REJECTED',
    batchId: null,
    retryCount: 1,
    failureReason: 'Document verification failed',
  },
};

export const FailedPermanent: Story = {
  args: {
    status: 'FAILED_PERMANENT',
    batchId: null,
    retryCount: 3,
    failureReason: 'Maximum retries exceeded - invalid employee data',
  },
};
