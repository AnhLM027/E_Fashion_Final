import random
import uuid
import datetime
import os
import subprocess

def run_query(query):
    cmd = ["docker", "exec", "-i", "e_fashion_db", "mysql", "-u", "root", "-proot_password", "e_fashion", "-N", "-e", query]
    result = subprocess.run(cmd, capture_output=True)
    if result.returncode != 0:
        print(f"Error running query: {result.stderr.decode('utf-8', errors='replace')}")
        return []
    # Decode with errors='replace' to avoid UnicodeDecodeError
    output = result.stdout.decode('utf-8', errors='replace').strip()
    if not output:
        return []
    return output.split('\n')

# Get users
users = run_query("SELECT id FROM users;")
if not users:
    print("No users found.")
    exit(1)

# Get products
product_data_raw = run_query("""
SELECT 
    pvs.id, 
    p.name, 
    c.name, 
    pvs.size_name, 
    pvs.sale_price
FROM product_variant_sizes pvs
JOIN product_variants pv ON pvs.variant_id = pv.id
JOIN products p ON pv.product_id = p.id
JOIN colors c ON pv.color_id = c.id
WHERE pvs.stock > 0
LIMIT 100;
""")

product_list = []
for line in product_data_raw:
    parts = line.split('\t')
    if len(parts) == 5:
        product_list.append({
            'pvs_id': parts[0],
            'product_name': parts[1].replace("'", "''"),
            'color_name': parts[2].replace("'", "''"),
            'size_name': parts[3].replace("'", "''"),
            'price': float(parts[4])
        })

if not product_list:
    print("No products found.")
    exit(1)

# Generate orders over the last 30 days
num_orders = 100
days_back = 60
end_date = datetime.date.today()

order_inserts = []
item_inserts = []

status_list = ['DELIVERED', 'SHIPPED', 'PROCESSING', 'PENDING']
payment_methods = ['COD', 'BANKING', 'VNPAY', 'MOMO']

for i in range(num_orders):
    order_id = str(uuid.uuid4())
    user_id = random.choice(users)
    
    # Pick 1-3 items for this order
    num_items = random.randint(1, 3)
    items = random.sample(product_list, num_items)
    
    total_price = 0
    for item in items:
        qty = random.randint(1, 2)
        price_at_purchase = item['price']
        total_price += price_at_purchase * qty
        
        item_id = str(uuid.uuid4())
        item_inserts.append(f"('{item_id}','{order_id}','{item['pvs_id']}','{item['product_name']}','{item['color_name']}','{item['size_name']}',{qty},{price_at_purchase})")

    status = random.choice(status_list)
    pay_method = random.choice(payment_methods)
    pay_status = 'PAID' if status == 'DELIVERED' else 'UNPAID'
    
    # Mock address info
    receiver_name = "Khách hàng " + str(i+1)
    receiver_phone = "09" + "".join([str(random.randint(0,9)) for _ in range(8)])
    province = "Hà Nội"
    district = "Cầu Giấy"
    ward = "Dịch Vọng"
    detail_address = str(random.randint(1, 100)) + " Đường Xuân Thủy"
    
    # Random time within the last 30 days
    random_days = random.randint(0, days_back)
    order_date = end_date - datetime.timedelta(days=random_days)
    hour = random.randint(0, 23) 
    minute = random.randint(0, 59)
    second = random.randint(0, 59)
    created_at = f"{order_date.isoformat()} {hour:02}:{minute:02}:{second:02}"
    
    order_inserts.append(f"('{order_id}','{user_id}','{receiver_name}','{receiver_phone}','{province}','{district}','{ward}','{detail_address}',0.00,0.00,NULL,{total_price},{total_price},'{status}','{pay_method}','{pay_status}',NULL,'{created_at}','{created_at}')")

# Execute inserts
def execute_sql(sql):
    cmd = ["docker", "exec", "-i", "e_fashion_db", "mysql", "-u", "root", "-proot_password", "e_fashion", "-e", sql]
    subprocess.run(cmd)

if order_inserts:
    print(f"Inserting {len(order_inserts)} orders...")
    # Using batches if needed, but 50 should be fine for one line.
    order_sql = "INSERT INTO orders (id, user_id, receiver_name, receiver_phone, province, district, ward, detail_address, shipping_fee, discount_amount, coupon_code, total_price, final_price, status, payment_method, payment_status, tracking_number, created_at, updated_at) VALUES " + ",".join(order_inserts) + ";"
    execute_sql(order_sql)

if item_inserts:
    print(f"Inserting {len(item_inserts)} items...")
    item_sql = "INSERT INTO order_items (id, order_id, product_variant_size_id, product_name, color_name, size_name, quantity, price_at_purchase) VALUES " + ",".join(item_inserts) + ";"
    execute_sql(item_sql)

print("Finished generating transaction data.")
