#!/bin/bash
# Test A: 5 simultaneous returns of the SAME loan (10 rounds).
# Test B: one member fires 5 simultaneous borrows of 5 different books (10 rounds).
BASE="http://localhost:8080/api"
ROUNDS=10
RUN=$(date +%s)
TMP=$(mktemp -d)

first_id() { grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4; }
post_json() { curl -s -X POST "$BASE/$1" -H 'Content-Type: application/json' -d "$2"; }
new_book() { post_json books "{\"authorId\":\"$AUTHOR_ID\",\"title\":\"$1\",\"isbn\":\"$2\",\"totalCopies\":1}" | first_id; }
new_member() { post_json members "{\"fullName\":\"$1\",\"email\":\"$2\"}" | first_id; }

AUTHOR_ID=$(post_json authors '{"name":"Race Author 2","nationality":"Test"}' | first_id)
if [ -z "$AUTHOR_ID" ]; then echo "Could not create an author. Is the app running?"; exit 1; fi

echo "== Test A: 5 simultaneous returns of the same loan =="
okA=0
for i in $(seq 1 $ROUNDS); do
  BOOK=$(new_book "Return Race $i" "RET-$RUN-$i")
  MEM=$(new_member "Returner $i" "returner-$RUN-$i@race.test")
  LOAN=$(curl -s -X POST "$BASE/loans/borrow?bookId=$BOOK&memberId=$MEM" | first_id)
  rm -f "$TMP"/r-*
  for j in 1 2 3 4 5; do
    ( curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/loans/$LOAN/return" > "$TMP/r-$j" ) &
  done
  wait
  okc=0; conflict=0; other=0
  for j in 1 2 3 4 5; do
    case $(cat "$TMP/r-$j") in 200) okc=$((okc+1));; 409) conflict=$((conflict+1));; *) other=$((other+1));; esac
  done
  copies=$(curl -s "$BASE/books/$BOOK" | grep -o '"availableCopies":[0-9-]*' | cut -d: -f2)
  if [ "$okc" -eq 1 ] && [ "$conflict" -eq 4 ] && [ "$other" -eq 0 ] && [ "$copies" = "1" ]; then s=OK; okA=$((okA+1)); else s=PROBLEM; fi
  echo "round $i: returned=$okc refused=$conflict other=$other availableCopies=$copies (should be 1) -> $s"
done

echo
echo "== Test B: 5 simultaneous borrows by ONE member (limit is 3) =="
okB=0
for i in $(seq 1 $ROUNDS); do
  MEM=$(new_member "Borrower $i" "borrower-$RUN-$i@race.test")
  books=()
  for j in 1 2 3 4 5; do books+=("$(new_book "Limit Race $i-$j" "LIM-$RUN-$i-$j")"); done
  rm -f "$TMP"/b-*
  for j in 1 2 3 4 5; do
    ( curl -s -o /dev/null -w '%{http_code}' -X POST \
        "$BASE/loans/borrow?bookId=${books[$((j-1))]}&memberId=$MEM" > "$TMP/b-$j" ) &
  done
  wait
  won=0; refused=0; other=0
  for j in 1 2 3 4 5; do
    case $(cat "$TMP/b-$j") in 201) won=$((won+1));; 409) refused=$((refused+1));; *) other=$((other+1));; esac
  done
  active=$(curl -s "$BASE/loans/member/$MEM" | grep -o '"returnDate":null' | wc -l | tr -d ' ')
  if [ "$won" -eq 3 ] && [ "$refused" -eq 2 ] && [ "$other" -eq 0 ] && [ "$active" = "3" ]; then s=OK; okB=$((okB+1)); else s=PROBLEM; fi
  echo "round $i: borrowed=$won refused=$refused other=$other activeLoans=$active (should be 3) -> $s"
done

echo
echo "Test A: $okA of $ROUNDS rounds correct"
echo "Test B: $okB of $ROUNDS rounds correct"
rm -rf "$TMP"
