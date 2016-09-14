-- :name create-customer! :! :n
-- :doc creates a new user record
INSERT INTO customers
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- :name update-customer! :! :n
-- :doc update an existing user record
UPDATE customers
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-customer :? :1
-- :doc retrieve a user given the id.
SELECT * FROM customers
WHERE id = :id

-- :name delete-customer! :! :n
-- :doc delete a user given the id
DELETE FROM customers
WHERE id = :id

-- :name get-customer-subsystems :? :*
-- :doc get subsystems for a customer given the id
select p.resource, p.action
from customers c
join roles r on r.customer_id=c.id
join grants g on g.role_id=r.id
join permissions p on p.id=g.permission_id
where r.account_id is null and c.id = :id

-- :name get-customer-account-subsystems :? :*
-- :doc get all accounts/subsystems for a given customer
select a.account_number, p.resource, p.action
from customers c
join roles r on r.customer_id=c.id
join accounts a on a.id=r.account_id
join grants g on g.role_id=r.id
join permissions p on p.id=g.permission_id
where c.id = :id
order by a.account_number
