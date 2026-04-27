import sys

file_path = r"h:\Project\Ecomm\Jhapcham Backend\jhapcham-Backend\src\main\java\com\example\jhapcham\product\ProductService.java"

with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Remove trailing closing braces and empty lines to find the insertion point
while lines and (lines[-1].strip() == '}' or lines[-1].strip() == ''):
    lines.pop()

# Add the closing brace for the last method if we popped it (we shouldn't have popped the method brace if it's not the class brace)
# But let's just append the missing method and the final class brace.

method_code = """
    private BigDecimal resolveEffectiveUnitPrice(Product p) {
        if (Boolean.TRUE.equals(p.getOnSale()) && p.getSalePrice() != null) {
            return p.getSalePrice();
        }
        return p.getPrice();
    }
}
"""

# Re-add the closing brace of ensureCategoryExists if we popped it
lines.append("    }\n")
lines.append(method_code)

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(lines)

print("Fixed ProductService.java")
