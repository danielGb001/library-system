#!/bin/bash
# Five members race for the single copy of a book, 20 times.
BASE="http://localhost:8080/api"
AUTH="${ADMIN_USERNAME:?Set ADMIN_USERNAME and ADMIN_PASSWORD first}:${ADMIN_PASSWORD:?Set ADMIN_USERNAME and ADMIN_PASSWORD first}"
ROUNDS=20
RACERS=5
RUN=$(date +%s)
TMP=$(mktemp -d)

first_id() { grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4; }
post_json() { curl -s -u "$AUTH" -X POST "$BASE/$1" -H 'Content-Type: application/json' -d "$2"; }

AUTHOR_ID=$(post_json authors '{"name":"Race Author","nationality":"Test"}' | first_id)
if [ -z "$AUTHOR_ID" ]; then
  echo "Could not create an author. Is the app running on port 8080?"
  exit 1
fi

ok_rounds=0
for i in $(seq 1 $ROUNDS); do
  BOOK_ID=$(post_json books "{\"authorId\":\"$AUTHOR_ID\",\"title\":\"Race Book $i\",\"isbn\":\"RACE-$RUN-$i\",\"totalCopies\":1}" | first_id)

  members=()
  for j in $(seq 1 $RACERS); do
    m=$(post_json members "{\"fullName\":\"Racer $i-$j\",\"email\":\"racer-$RUN-$i-$j@race.test\"}" | first_id)
    members+=("$m")
  done

  rm -f "$TMP"/code-*
  for j in $(seq 1 $RACERS); do
    ( curl -s -o /dev/null -w '%{http_code}' -u "$AUTH" -X POST \
        "$BASE/loans/borrow?bookId=$BOOK_ID&memberId=${members[$((j-1))]}" > "$TMP/code-$j" ) &
  done
  wait

  won=0; refused=0; other=0; others=""
  for j in $(seq 1 $RACERS); do
    code=$(cat "$TMP/code-$j")
    case $code in
      201) won=$((won+1)) ;;
      409) refused=$((refused+1)) ;;
      *)   other=$((other+1)); others="$others $code" ;;
    esac
  done

  copies=$(curl -s "$BASE/books/$BOOK_ID" | grep -o '"availableCopies":[0-9-]*' | cut -d: -f2)

  if [ "$won" -eq 1 ] && [ "$copies" = "0" ] && [ "$other" -eq 0 ]; then
    status=OK; ok_rounds=$((ok_rounds+1))
  else
    status=PROBLEM
  fi
  echo "round $i: borrowed=$won refused=$refused other=$other$others availableCopies=$copies -> $status"
done

echo
echo "$ok_rounds of $ROUNDS rounds correct"
rm -rf "$TMP"
