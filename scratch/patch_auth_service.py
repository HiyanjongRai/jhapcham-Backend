import os

path = r"H:\Project\Ecomm\Jhapcham Backend\jhapcham-Backend\src\main\java\com\example\jhapcham\user\model\AuthService.java"

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Target block
old_block = """        if (user.getRole() == Role.SELLER) {
            if (user.getStatus() == Status.PENDING) {
                boolean hasApplication = applicationRepository.existsByUser(user);
                if (hasApplication) {
                    throw new AuthorizationException("Your application is pending approval");
                }
            }
            if (user.getStatus() == Status.BLOCKED) {
                throw new AuthorizationException("Your account is blocked, contact support");
            }
        }"""

new_block = """        if (user.getRole() == Role.SELLER) {
            // Allow PENDING sellers to login so they can see their status page
            if (user.getStatus() == Status.BLOCKED) {
                throw new AuthorizationException("Your account is blocked, contact support");
            }
        }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Success")
else:
    # Try with slightly different whitespace if it failed
    import re
    # Match the logic regardless of exact spacing
    pattern = r'if \(user\.getRole\(\) == Role\.SELLER\) \{(?:\s+if \(user\.getStatus\(\) == Status\.PENDING\) \{.*?\n\s+\})?\s+if \(user\.getStatus\(\) == Status\.BLOCKED\) \{.*?\n\s+\}\s+\}'
    # This is complex, let's just use substrings
    if 'if (user.getRole() == Role.SELLER)' in content:
        # Just find the whole method validateUserStatus
        start_marker = 'private User validateUserStatus(User user) {'
        end_marker = 'return user;'
        if start_marker in content and end_marker in content:
            method_start = content.find(start_marker)
            method_end = content.find(end_marker, method_start) + len(end_marker)
            
            new_method = """private User validateUserStatus(User user) {
        if (user.getRole() == Role.SELLER) {
            if (user.getStatus() == Status.BLOCKED) {
                throw new AuthorizationException("Your account is blocked, contact support");
            }
        } else {
            if (user.getStatus() != Status.ACTIVE) {
                throw new AuthorizationException("Account is not active");
            }
        }
        return user;
    }"""
            content = content[:method_start] + new_method + content[method_end:]
            with open(path, 'w', encoding='utf-8') as f:
                f.write(content)
            print("Success (Method replaced)")
        else:
            print("Markers not found")
    else:
        print("Role check not found")
