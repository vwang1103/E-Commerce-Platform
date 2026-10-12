import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { QuantityStepper } from "@/shared/components/QuantityStepper";

function Harness({ max = 3 }: Readonly<{ max?: number }>) {
  const [value, setValue] = useState(1);
  return <QuantityStepper value={value} onChange={setValue} max={max} />;
}

describe("QuantityStepper", () => {
  it("stays within its bounds", async () => {
    render(<Harness />);
    const decrease = screen.getByRole("button", { name: /decrease/i });
    const increase = screen.getByRole("button", { name: /increase/i });

    expect(decrease).toBeDisabled();
    await userEvent.click(increase);
    await userEvent.click(increase);
    expect(screen.getByRole("status")).toHaveTextContent("3");
    expect(increase).toBeDisabled();
    await userEvent.click(decrease);
    expect(screen.getByRole("status")).toHaveTextContent("2");
  });
});
