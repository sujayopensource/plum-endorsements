import type { Meta, StoryObj } from '@storybook/react';
import { TypeBadge } from './TypeBadge';

const meta: Meta<typeof TypeBadge> = {
  title: 'Shared/TypeBadge',
  component: TypeBadge,
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof TypeBadge>;

export const Add: Story = { args: { type: 'ADD' } };
export const Delete: Story = { args: { type: 'DELETE' } };
export const Update: Story = { args: { type: 'UPDATE' } };
