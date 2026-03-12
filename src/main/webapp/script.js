const emailLabel = document.querySelector("#loginEmailLabel");
const email = document.querySelector("#loginEmail");
const password = document.querySelector("#loginPassword");
const showPasswordCheck = document.querySelector("#showPasswordCheck");
const showPasswordToggle = document.querySelector("#showPasswordToggle");
const mySVG = document.querySelector(".svgContainer");

const armL = document.querySelector(".armL");
const armR = document.querySelector(".armR");
const twoFingers = document.querySelector(".twoFingers");

const eyeL = document.querySelector(".eyeL");
const eyeR = document.querySelector(".eyeR");
const nose = document.querySelector(".nose");
const mouth = document.querySelector(".mouth");
const chin = document.querySelector(".chin");
const face = document.querySelector(".face");
const eyebrow = document.querySelector(".eyebrow");
const outerEarL = document.querySelector(".earL .outerEar");
const outerEarR = document.querySelector(".earR .outerEar");
const earHairL = document.querySelector(".earL .earHair");
const earHairR = document.querySelector(".earR .earHair");
const hair = document.querySelector(".hair");
const bodyBGnormal = document.querySelector(".bodyBGnormal");
const bodyBGchanged = document.querySelector(".bodyBGchanged");

let activeElement = null;
let screenCenter, svgCoords, emailCoords;
let eyeLCoords, eyeRCoords, noseCoords, mouthCoords;
let blinking = null;
let eyeScale = 1;
let eyesCovered = false;
let showPasswordClicked = false;
let chinMin = 0.5;

function hasElements() {
  return email && password && mySVG && armL && armR && eyeL && eyeR &&
    nose && mouth && chin && face && eyebrow &&
    outerEarL && outerEarR && earHairL && earHairR && hair;
}

function getAngle(x1, y1, x2, y2) {
  return Math.atan2(y1 - y2, x1 - x2);
}

function getPosition(el) {
  let xPos = 0;
  let yPos = 0;

  while (el) {
    if (el.tagName === "BODY") {
      const xScroll = el.scrollLeft || document.documentElement.scrollLeft;
      const yScroll = el.scrollTop || document.documentElement.scrollTop;
      xPos += el.offsetLeft - xScroll + el.clientLeft;
      yPos += el.offsetTop - yScroll + el.clientTop;
    } else {
      xPos += el.offsetLeft - el.scrollLeft + el.clientLeft;
      yPos += el.offsetTop - el.scrollTop + el.clientTop;
    }
    el = el.offsetParent;
  }

  return { x: xPos, y: yPos };
}

function getRandomInt(max) {
  return Math.floor(Math.random() * Math.floor(max));
}

function startBlinking(delay) {
  if (!eyeL || !eyeR || typeof TweenMax === "undefined") return;

  const d = delay ? getRandomInt(delay) : 1;
  blinking = TweenMax.to([eyeL, eyeR], 0.1, {
    delay: d,
    scaleY: 0,
    yoyo: true,
    repeat: 1,
    transformOrigin: "center center",
    onComplete: function () {
      startBlinking(12);
    }
  });
}

function stopBlinking() {
  if (!blinking) return;
  blinking.kill();
  blinking = null;
  if (typeof TweenMax !== "undefined") {
    TweenMax.set([eyeL, eyeR], { scaleY: eyeScale });
  }
}

function recalcCoords() {
  if (!email || !mySVG) return;

  svgCoords = getPosition(mySVG);
  emailCoords = getPosition(email);
  screenCenter = svgCoords.x + mySVG.offsetWidth / 2;

  eyeLCoords = { x: svgCoords.x + 84, y: svgCoords.y + 76 };
  eyeRCoords = { x: svgCoords.x + 113, y: svgCoords.y + 76 };
  noseCoords = { x: svgCoords.x + 97, y: svgCoords.y + 81 };
  mouthCoords = { x: svgCoords.x + 100, y: svgCoords.y + 100 };
}

function calculateFaceMove() {
  if (!hasElements() || typeof TweenMax === "undefined") return;

  let carPos = email.selectionEnd;
  if (carPos == null) carPos = email.value.length;

  const div = document.createElement("div");
  const span = document.createElement("span");
  const copyStyle = getComputedStyle(email);

  Array.from(copyStyle).forEach(function (prop) {
    div.style[prop] = copyStyle[prop];
  });

  div.style.position = "absolute";
  div.style.visibility = "hidden";
  div.style.whiteSpace = "pre";
  div.style.top = "0px";
  div.style.left = "0px";
  div.style.width = email.offsetWidth + "px";
  div.style.height = email.offsetHeight + "px";

  document.body.appendChild(div);
  div.textContent = email.value.substring(0, carPos);
  span.textContent = email.value.substring(carPos) || ".";
  div.appendChild(span);

  const caretX = span.offsetLeft;
  const targetX = emailCoords.x + caretX;
  const targetY = emailCoords.y + 25;

  const dFromC = screenCenter - targetX;

  const eyeLAngle = getAngle(eyeLCoords.x, eyeLCoords.y, targetX, targetY);
  const eyeRAngle = getAngle(eyeRCoords.x, eyeRCoords.y, targetX, targetY);
  const noseAngle = getAngle(noseCoords.x, noseCoords.y, targetX, targetY);
  const mouthAngle = getAngle(mouthCoords.x, mouthCoords.y, targetX, targetY);

  const eyeLX = Math.cos(eyeLAngle) * 20;
  const eyeLY = Math.sin(eyeLAngle) * 10;
  const eyeRX = Math.cos(eyeRAngle) * 20;
  const eyeRY = Math.sin(eyeRAngle) * 10;

  const noseX = Math.cos(noseAngle) * 23;
  const noseY = Math.sin(noseAngle) * 10;
  const mouthX = Math.cos(mouthAngle) * 23;
  const mouthY = Math.sin(mouthAngle) * 10;
  const mouthR = Math.cos(mouthAngle) * 6;

  const chinX = mouthX * 0.8;
  const chinY = mouthY * 0.5;

  let chinS = 1 - (dFromC * 0.15) / 100;
  if (chinS > 1) {
    chinS = 1 - (chinS - 1);
    if (chinS < chinMin) chinS = chinMin;
  }

  const faceX = mouthX * 0.3;
  const faceY = mouthY * 0.4;
  const faceSkew = Math.cos(mouthAngle) * 5;
  const eyebrowSkew = Math.cos(mouthAngle) * 25;
  const outerEarX = Math.cos(mouthAngle) * 4;
  const outerEarY = Math.cos(mouthAngle) * 5;
  const hairX = Math.cos(mouthAngle) * 6;

  TweenMax.to(eyeL, 1, { x: -eyeLX, y: -eyeLY, ease: Expo.easeOut });
  TweenMax.to(eyeR, 1, { x: -eyeRX, y: -eyeRY, ease: Expo.easeOut });

  TweenMax.to(nose, 1, {
    x: -noseX,
    y: -noseY,
    rotation: mouthR,
    transformOrigin: "center center",
    ease: Expo.easeOut
  });

  TweenMax.to(mouth, 1, {
    x: -mouthX,
    y: -mouthY,
    rotation: mouthR,
    transformOrigin: "center center",
    ease: Expo.easeOut
  });

  TweenMax.to(chin, 1, {
    x: -chinX,
    y: -chinY,
    scaleY: chinS,
    ease: Expo.easeOut
  });

  TweenMax.to(face, 1, {
    x: -faceX,
    y: -faceY,
    skewX: -faceSkew,
    transformOrigin: "center top",
    ease: Expo.easeOut
  });

  TweenMax.to(eyebrow, 1, {
    x: -faceX,
    y: -faceY,
    skewX: -eyebrowSkew,
    transformOrigin: "center top",
    ease: Expo.easeOut
  });

  TweenMax.to(outerEarL, 1, { x: outerEarX, y: -outerEarY, ease: Expo.easeOut });
  TweenMax.to(outerEarR, 1, { x: outerEarX, y: outerEarY, ease: Expo.easeOut });
  TweenMax.to(earHairL, 1, { x: -outerEarX, y: -outerEarY, ease: Expo.easeOut });
  TweenMax.to(earHairR, 1, { x: -outerEarX, y: outerEarY, ease: Expo.easeOut });
  TweenMax.to(hair, 1, {
    x: hairX,
    scaleY: 1.2,
    transformOrigin: "center bottom",
    ease: Expo.easeOut
  });

  document.body.removeChild(div);
}

function resetFace() {
  if (!hasElements() || typeof TweenMax === "undefined") return;

  TweenMax.to([eyeL, eyeR], 1, { x: 0, y: 0, scaleY: 1, ease: Expo.easeOut });
  TweenMax.to(nose, 1, { x: 0, y: 0, rotation: 0, scaleX: 1, scaleY: 1, ease: Expo.easeOut });
  TweenMax.to(mouth, 1, { x: 0, y: 0, rotation: 0, ease: Expo.easeOut });
  TweenMax.to(chin, 1, { x: 0, y: 0, scaleY: 1, ease: Expo.easeOut });
  TweenMax.to([face, eyebrow], 1, { x: 0, y: 0, skewX: 0, ease: Expo.easeOut });
  TweenMax.to([outerEarL, outerEarR, earHairL, earHairR, hair], 1, {
    x: 0,
    y: 0,
    scaleY: 1,
    ease: Expo.easeOut
  });
}

function coverEyes() {
  if (!armL || !armR || typeof TweenMax === "undefined") return;

  TweenMax.killTweensOf([armL, armR]);
  TweenMax.set([armL, armR], { visibility: "visible" });

  TweenMax.to(armL, 0.45, { x: -93, y: 11, rotation: 0, ease: Quad.easeOut });
  TweenMax.to(armR, 0.45, { x: -93, y: 11, rotation: 0, ease: Quad.easeOut, delay: 0.1 });

  if (bodyBGnormal && bodyBGchanged) {
    bodyBGnormal.style.display = "none";
    bodyBGchanged.style.display = "block";
  }

  eyesCovered = true;
}

function uncoverEyes() {
  if (!armL || !armR || typeof TweenMax === "undefined") return;

  TweenMax.killTweensOf([armL, armR]);

  TweenMax.to(armL, 1.35, { y: 220, ease: Quad.easeOut });
  TweenMax.to(armL, 1.35, { rotation: 105, ease: Quad.easeOut, delay: 0.1 });

  TweenMax.to(armR, 1.35, { y: 220, ease: Quad.easeOut });
  TweenMax.to(armR, 1.35, {
    rotation: -105,
    ease: Quad.easeOut,
    delay: 0.1,
    onComplete: function () {
      TweenMax.set([armL, armR], { visibility: "hidden" });
    }
  });

  if (bodyBGnormal && bodyBGchanged) {
    bodyBGnormal.style.display = "block";
    bodyBGchanged.style.display = "none";
  }

  eyesCovered = false;
}

function spreadFingers() {
  if (!twoFingers || typeof TweenMax === "undefined") return;
  TweenMax.to(twoFingers, 0.35, {
    transformOrigin: "bottom left",
    rotation: 30,
    x: -9,
    y: -2,
    ease: Power2.easeInOut
  });
}

function closeFingers() {
  if (!twoFingers || typeof TweenMax === "undefined") return;
  TweenMax.to(twoFingers, 0.35, {
    transformOrigin: "bottom left",
    rotation: 0,
    x: 0,
    y: 0,
    ease: Power2.easeInOut
  });
}

function onEmailFocus(e) {
  activeElement = "email";
  e.target.parentElement.classList.add("focusWithText");
  stopBlinking();
  recalcCoords();
  uncoverEyes();
  calculateFaceMove();
}

function onEmailInput() {
  calculateFaceMove();
}

function onEmailBlur(e) {
  activeElement = null;
  setTimeout(function () {
    if (activeElement === "email") return;
    if (e.target.value === "") {
      e.target.parentElement.classList.remove("focusWithText");
    }
    startBlinking(5);
    resetFace();
  }, 100);
}

function onPasswordFocus() {
  activeElement = "password";
  coverEyes();
}

function onPasswordBlur() {
  activeElement = null;
  setTimeout(function () {
    if (activeElement === "toggle" || activeElement === "password") return;
    closeFingers();
    uncoverEyes();
  }, 120);
}

function onPasswordToggleFocus() {
  activeElement = "toggle";
  coverEyes();
}

function onPasswordToggleBlur() {
  activeElement = null;
  if (!showPasswordClicked) {
    setTimeout(function () {
      if (activeElement === "password" || activeElement === "toggle") return;
      closeFingers();
      uncoverEyes();
    }, 100);
  }
}

function onPasswordToggleMouseDown() {
  showPasswordClicked = true;
}

function onPasswordToggleMouseUp() {
  showPasswordClicked = false;
}

function onPasswordToggleChange(e) {
  setTimeout(function () {
    coverEyes();

    if (e.target.checked) {
      password.type = "text";
      spreadFingers();
    } else {
      password.type = "password";
      closeFingers();
    }
  }, 50);
}

function initLoginForm() {
  if (!email || !password || !showPasswordCheck || !showPasswordToggle) return;

  recalcCoords();

  email.addEventListener("focus", onEmailFocus);
  email.addEventListener("blur", onEmailBlur);
  email.addEventListener("input", onEmailInput);

  if (emailLabel) {
    emailLabel.addEventListener("click", function () {
      activeElement = "email";
    });
  }

  password.addEventListener("focus", onPasswordFocus);
  password.addEventListener("blur", onPasswordBlur);

  showPasswordCheck.addEventListener("change", onPasswordToggleChange);
  showPasswordCheck.addEventListener("focus", onPasswordToggleFocus);
  showPasswordCheck.addEventListener("blur", onPasswordToggleBlur);

  showPasswordToggle.addEventListener("mouseup", onPasswordToggleMouseUp);
  showPasswordToggle.addEventListener("mousedown", onPasswordToggleMouseDown);

  if (typeof TweenMax !== "undefined") {
    if (armL) TweenMax.set(armL, { x: -93, y: 220, rotation: 105, transformOrigin: "top left" });
    if (armR) TweenMax.set(armR, { x: -93, y: 220, rotation: -105, transformOrigin: "top right" });
    if (mouth) TweenMax.set(mouth, { transformOrigin: "center center" });
  }

  if (bodyBGnormal && bodyBGchanged) {
    bodyBGnormal.style.display = "block";
    bodyBGchanged.style.display = "none";
  }

  startBlinking(5);
  resetFace();
  uncoverEyes();
}

function attachLoginSubmit() {
  const form = document.getElementById("loginForm");
  if (!form) return;

  form.addEventListener("submit", async function (e) {
    e.preventDefault();

    const emailVal = email ? email.value.trim() : "";
    const passVal = password ? password.value : "";

    if (!emailVal || !passVal) {
      alert("Please enter email and password");
      return;
    }

    const params = new URLSearchParams();
    params.append("email", emailVal);
    params.append("password", passVal);

    try {
      const res = await fetch("./login", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params.toString()
      });

      const raw = await res.text();
      console.log("Login status:", res.status);
      console.log("Login raw response:", raw);

      let data;
      try {
        data = JSON.parse(raw);
      } catch (err) {
        alert("Login servlet not returning JSON.\nHTTP " + res.status + "\n\n" + raw);
        return;
      }

      if (data.success) {
        alert(data.message || "Login successful");
        window.location.href = "./room.html";
      } else {
        let msg = data.message || "Login failed";

        if (
          typeof msg === "string" &&
          msg.toLowerCase() === "invalid password" &&
          data.remainingAttempts !== undefined &&
          data.remainingAttempts !== null
        ) {
          msg += "\nAttempts left: " + data.remainingAttempts;
        }

        alert(msg);
      }
    } catch (err) {
      console.error("Login fetch failed:", err);
      alert("Login request failed:\n" + err.message);
    }
  });
}

function attachSignupSubmit() {
  const form = document.getElementById("signupForm");
  if (!form) return;

  form.addEventListener("submit", async function (e) {
    e.preventDefault();

    const signupName = document.getElementById("signupName");
    const signupEmail = document.getElementById("signupEmail");
    const signupPassword = document.getElementById("signupPassword");
    const signupConfirmPassword = document.getElementById("signupConfirmPassword");

    const fullName = signupName ? signupName.value.trim() : "";
    const emailVal = signupEmail ? signupEmail.value.trim() : "";
    const passVal = signupPassword ? signupPassword.value : "";
    const confirmVal = signupConfirmPassword ? signupConfirmPassword.value : "";

    if (!fullName || !emailVal || !passVal || !confirmVal) {
      alert("Please fill all fields");
      return;
    }

    if (passVal !== confirmVal) {
      alert("Passwords do not match");
      return;
    }

    const params = new URLSearchParams();
    params.append("fullName", fullName);
    params.append("email", emailVal);
    params.append("password", passVal);

    try {
      const res = await fetch("./signup", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params.toString()
      });

      const raw = await res.text();
      console.log("Signup status:", res.status);
      console.log("Signup raw response:", raw);

      let data;
      try {
        data = JSON.parse(raw);
      } catch (err) {
        alert("Signup servlet not returning JSON.\nHTTP " + res.status + "\n\n" + raw);
        return;
      }

      if (data.success) {
        alert(data.message || "Account created successfully");
        window.location.href = "./index.html";
      } else {
        alert(data.message || "Signup failed");
      }
    } catch (err) {
      console.error("Signup fetch failed:", err);
      alert("Signup request failed:\n" + err.message);
    }
  });
}

window.addEventListener("load", function () {
  initLoginForm();
  attachLoginSubmit();
  attachSignupSubmit();
});

window.addEventListener("resize", function () {
  setTimeout(function () {
    recalcCoords();
    resetFace();
  }, 100);
});