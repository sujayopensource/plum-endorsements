import type { Meta, StoryObj } from '@storybook/react';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { Sidebar } from './Sidebar';

const meta: Meta<typeof Sidebar> = {
  title: 'Layout/Sidebar',
  component: Sidebar,
  tags: ['autodocs'],
  decorators: [
    (Story) => React.createElement('div', { style: { height: '400px' } }, React.createElement(Story)),
  ],
};

export default meta;
type Story = StoryObj<typeof Sidebar>;

export const DashboardActive: Story = {
  args: { mobile: true },
  decorators: [
    (Story) => React.createElement(MemoryRouter, { initialEntries: ['/'] }, React.createElement(Story)),
  ],
};

export const EndorsementsActive: Story = {
  args: { mobile: true },
  decorators: [
    (Story) => React.createElement(MemoryRouter, { initialEntries: ['/endorsements'] }, React.createElement(Story)),
  ],
};

export const EAAccountsActive: Story = {
  args: { mobile: true },
  decorators: [
    (Story) => React.createElement(MemoryRouter, { initialEntries: ['/ea-accounts'] }, React.createElement(Story)),
  ],
};
