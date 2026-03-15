import type { Meta, StoryObj } from '@storybook/react';
import React from 'react';
import { PageHeader } from './PageHeader';

const meta: Meta<typeof PageHeader> = {
  title: 'Shared/PageHeader',
  component: PageHeader,
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof PageHeader>;

export const Default: Story = {
  args: {
    title: 'Dashboard',
    description: 'Overview of endorsement operations',
  },
};

export const TitleOnly: Story = {
  args: {
    title: 'Endorsements',
  },
};

export const WithAction: Story = {
  args: {
    title: 'Endorsements',
    description: 'Manage employee endorsement requests',
    action: React.createElement(
      'button',
      {
        style: {
          padding: '8px 16px',
          borderRadius: '6px',
          backgroundColor: '#0f172a',
          color: '#fff',
          border: 'none',
          cursor: 'pointer',
          fontSize: '14px',
        },
      },
      'Create Endorsement'
    ),
  },
};
