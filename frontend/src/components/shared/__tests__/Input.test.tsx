import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Input, Select } from '../Input';

describe('Input Component', () => {
  it('renders input with label', () => {
    render(<Input label="Email" />);
    expect(screen.getByText('Email')).toBeInTheDocument();
  });

  it('renders input without label', () => {
    render(<Input placeholder="Enter email" />);
    expect(screen.getByPlaceholderText('Enter email')).toBeInTheDocument();
  });

  it('displays error message when error prop is provided', () => {
    render(<Input label="Email" error="Email is required" />);
    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Email is required')).toHaveClass('text-red-600');
  });

  it('displays helper text when helperText prop is provided', () => {
    render(<Input label="Email" helperText="Enter a valid email address" />);
    expect(screen.getByText('Enter a valid email address')).toBeInTheDocument();
    expect(screen.getByText('Enter a valid email address')).toHaveClass('text-gray-500');
  });

  it('prioritizes error over helper text', () => {
    render(
      <Input
        label="Email"
        error="Email is required"
        helperText="Enter a valid email address"
      />
    );
    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.queryByText('Enter a valid email address')).not.toBeInTheDocument();
  });

  it('handles value change', () => {
    const handleChange = jest.fn();
    render(<Input label="Name" onChange={handleChange} />);

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'John Doe' } });

    expect(handleChange).toHaveBeenCalled();
  });

  it('applies error border when error is present', () => {
    render(<Input label="Email" error="Invalid email" />);
    const input = screen.getByRole('textbox');
    expect(input).toHaveClass('border-red-300');
  });

  it('renders as disabled', () => {
    render(<Input label="Email" disabled />);
    const input = screen.getByRole('textbox');
    expect(input).toBeDisabled();
    expect(input).toHaveClass('bg-gray-100', 'cursor-not-allowed');
  });
});

describe('Select Component', () => {
  const options = [
    { value: 'option1', label: 'Option 1' },
    { value: 'option2', label: 'Option 2' },
    { value: 'option3', label: 'Option 3' },
  ];

  it('renders select with label', () => {
    render(<Select label="Choose Option" options={options} />);
    expect(screen.getByText('Choose Option')).toBeInTheDocument();
  });

  it('renders all options', () => {
    render(<Select label="Choose Option" options={options} />);
    expect(screen.getByText('Option 1')).toBeInTheDocument();
    expect(screen.getByText('Option 2')).toBeInTheDocument();
    expect(screen.getByText('Option 3')).toBeInTheDocument();
  });

  it('handles selection change', () => {
    const handleChange = jest.fn();
    render(<Select label="Choose Option" options={options} onChange={handleChange} />);

    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: 'option2' } });

    expect(handleChange).toHaveBeenCalled();
  });

  it('displays error message', () => {
    render(<Select label="Choose Option" options={options} error="Selection is required" />);
    expect(screen.getByText('Selection is required')).toBeInTheDocument();
  });

  it('renders as disabled', () => {
    render(<Select label="Choose Option" options={options} disabled />);
    const select = screen.getByRole('combobox');
    expect(select).toBeDisabled();
  });
});
