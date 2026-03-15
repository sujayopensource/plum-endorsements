import type { Meta, StoryObj } from '@storybook/react';
import { fn } from 'storybook/test';
import { EndorsementFilters } from './EndorsementFilters';

const meta: Meta<typeof EndorsementFilters> = {
  title: 'Endorsements/EndorsementFilters',
  component: EndorsementFilters,
  tags: ['autodocs'],
  args: {
    onEmployerIdChange: fn(),
    onStatusesChange: fn(),
  },
};

export default meta;
type Story = StoryObj<typeof EndorsementFilters>;

export const Default: Story = {
  args: {
    employerId: '11111111-1111-1111-1111-111111111111',
    selectedStatuses: [],
  },
};

export const WithSelectedStatuses: Story = {
  args: {
    employerId: '11111111-1111-1111-1111-111111111111',
    selectedStatuses: ['CONFIRMED', 'REJECTED'],
  },
};

export const Empty: Story = {
  args: {
    employerId: '',
    selectedStatuses: [],
  },
};
