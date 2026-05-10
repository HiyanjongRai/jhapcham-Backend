import os

path = r"H:\Project\Ecomm\Jhapcham Backend\jhapcham-Backend\src\main\java\com\example\jhapcham\seller\SellerApplicationService.java"

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update role check
old_check = 'if (seller.getRole() != Role.SELLER) {'
new_check = 'if (seller.getRole() != Role.SELLER && seller.getRole() != Role.CUSTOMER) {'
content = content.replace(old_check, new_check)

# 2. Add promotion logic
old_return = 'return applicationRepo.save(app);'
new_return = """// If user was a CUSTOMER, promote them to SELLER status PENDING
        if (seller.getRole() == Role.CUSTOMER) {
            seller.setRole(Role.SELLER);
            seller.setStatus(Status.PENDING);
            userRepo.save(seller);
        }

        return applicationRepo.save(app);"""

# Only replace the last occurrence in the submitApplication method
# (Actually, let's just replace 'return applicationRepo.save(app);' inside the method)
# Find the end of setReviewNote(null);
marker = 'app.setReviewNote(null); // Clear previous rejection reason'
if marker in content:
    parts = content.split(marker)
    # We expect marker to be in submitApplication
    # The return is shortly after
    if 'return applicationRepo.save(app);' in parts[1]:
        parts[1] = parts[1].replace('return applicationRepo.save(app);', new_return, 1)
        content = marker.join(parts)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Success")
