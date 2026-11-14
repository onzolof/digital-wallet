import { render, screen } from '@testing-library/react'
import { expect, test } from 'vitest'
import App from '@/App'

test('says hello', () => {
  render(<App />)
  expect(
    screen.getByRole('heading', { name: 'Hello world' }),
  ).toBeInTheDocument()
})
