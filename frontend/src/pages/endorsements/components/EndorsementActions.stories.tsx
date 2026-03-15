import type { Meta, StoryObj } from '@storybook/react';
import { EndorsementActions } from './EndorsementActions';

const meta: Meta<typeof EndorsementActions> = {
  title: 'Endorsements/EndorsementActions',
  component: EndorsementActions,
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof EndorsementActions>;

export const SubmitAvailable: Story = {
  args: {
    id: 'test-id-1',
    status: 'PROVISIONALLY_COVERED',
    retryCount: 0,
  },
};

export const ConfirmRejectAvailable: Story = {
  args: {
    id: 'test-id-2',
    status: 'SUBMITTED_REALTIME',
    retryCount: 0,
  },
};

export const RetryAvailable: Story = {
  args: {
    id: 'test-id-3',
    status: 'REJECTED',
    retryCount: 1,
  },
};

export const TerminalConfirmed: Story = {
  args: {
    id: 'test-id-4',
    status: 'CONFIRMED',
    retryCount: 0,
  },
};

export const TerminalFailed: Story = {
  args: {
    id: 'test-id-5',
    status: 'FAILED_PERMANENT',
    retryCount: 3,
  },
};
