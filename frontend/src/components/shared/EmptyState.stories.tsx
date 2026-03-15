import type { Meta, StoryObj } from '@storybook/react';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { FileText } from 'lucide-react';
import { EmptyState } from './EmptyState';

const meta: Meta<typeof EmptyState> = {
  title: 'Shared/EmptyState',
  component: EmptyState,
  tags: ['autodocs'],
  decorators: [
    (Story) => React.createElement(MemoryRouter, null, React.createElement(Story)),
  ],
};

export default meta;
type Story = StoryObj<typeof EmptyState>;

export const Default: Story = {
  args: {
    icon: FileText,
    title: 'No endorsements found',
    description: 'Create your first endorsement to get started.',
  },
};

export const WithAction: Story = {
  args: {
    icon: FileText,
    title: 'No endorsements found',
    description: 'Create your first endorsement to get started, or adjust your filters.',
    actionLabel: 'Create Endorsement',
    actionHref: '/endorsements/new',
  },
};

export const WithCallback: Story = {
  args: {
    icon: FileText,
    title: 'No results',
    description: 'Try adjusting your search criteria.',
    actionLabel: 'Reset Filters',
    onAction: () => console.log('Reset clicked'),
  },
};
